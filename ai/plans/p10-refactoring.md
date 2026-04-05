# П10 — Детальный рефакторинг всех модулей

## Контекст

MVP готов: 8 bounded contexts, 227 тестов, CI/CD настроен. Данный план — системный рефакторинг
на основе полного аудита кодовой базы. Цель: устранить технический долг, укрепить безопасность,
устранить нарушения гексагональной архитектуры и повысить качество кода до production-уровня.

**Почему**: без рефакторинга технический долг накапливается. Критические проблемы (auth→org
нарушение, отсутствие фильтрации задач по org, молчаливое проглатывание JWT-ошибок) создают
уязвимости безопасности и могут привести к утечке данных между тенантами.

---

## Фаза 1 — КРИТИЧНО: Безопасность и корректность

### 1.1 Утечка данных между тенантами в TaskController

**Файл**: `task/adapter/in/web/controller/TaskController.java`

**Проблема**: `POST /api/tasks` принимает задачи от любого аутентифицированного пользователя,
но не привязывает задачу к org/project вошедшего пользователя. `GET /api/tasks` не фильтрует
задачи по org пользователя — пользователь org A может видеть задачи org B.

**Решение**:
```java
// POST /api/tasks — добавить projectId из principal
@PostMapping
public ResponseEntity<TaskResponse> create(
    @Valid @RequestBody CreateTaskRequest req,
    @AuthenticationPrincipal AuthenticatedUser principal) {
  var taskId = agentOrchestrator.submit(
      req.title(), req.description(), req.agentRole(), principal.orgId());
  // ...
}

// GET /api/tasks — фильтровать по org
@GetMapping
public List<TaskResponse> listByOrg(
    @AuthenticationPrincipal AuthenticatedUser principal) {
  return taskQueryService.getByOrg(principal.orgId());
}
```

**Миграция**: добавить `org_id` как FK в `tasks` (через project→org, но для производительности
лучше денормализация). Либо использовать `JOIN tasks → projects → organizations`.

**TaskStore** — добавить метод:
```java
List<TaskModel> findByOrgId(UUID orgId);
```

**Acceptance Criteria**:
- [ ] `GET /api/tasks` возвращает только задачи org текущего пользователя
- [ ] `POST /api/tasks` привязывает задачу к org пользователя
- [ ] Тест: пользователь org A не видит задачи org B

---

### 1.2 Нарушение границ: AuthService создаёт Organization

**Файл**: `auth/app/service/AuthService.java`

**Проблема**: `auth` bounded context напрямую вызывает `OrganizationCommandService.createOrganization()`.
Нарушает принцип `auth → domain`, создаёт циклическую зависимость при расширении.
Создание организации — это не ответственность auth.

**Решение**: ввести port в `auth/domain/`:
```java
// auth/domain/OrganizationCreationPort.java
public interface OrganizationCreationPort {
  UUID createForUser(String name);
}
```

Реализация в `organization/adapter/in/hook/`:
```java
// organization/adapter/in/hook/OrganizationCreationAdapter.java
@Component
public class OrganizationCreationAdapter implements OrganizationCreationPort {
  private final OrganizationCommandService commandService;

  @Override
  public UUID createForUser(String name) {
    return commandService.createOrganization(name).id();
  }
}
```

`AuthService` инжектирует `OrganizationCreationPort`, не `OrganizationCommandService`.

**Acceptance Criteria**:
- [ ] `AuthService` не импортирует ничего из `organization.*`
- [ ] `OrganizationCreationPort` в `auth/domain/`
- [ ] ArchUnit тест не ломается
- [ ] Регистрация пользователя по-прежнему создаёт org

---

### 1.3 JwtAuthFilter молча глотает ошибки

**Файл**: `bootstrap/JwtAuthFilter.java`

**Проблема**: Catch-all exception handler в JwtAuthFilter не логирует тип ошибки
и не различает «токен просрочен» от «токен подделан». Злоумышленник может пробовать
разные токены без каких-либо следов в логах.

**Решение**:
```java
} catch (ExpiredJwtException e) {
  log.debug("JWT токен просрочен: {}", e.getMessage());
} catch (MalformedJwtException | SecurityException e) {
  log.warn("Невалидный JWT токен: {}", e.getMessage());
} catch (Exception e) {
  log.error("Непредвиденная ошибка при валидации JWT", e);
}
```

**Acceptance Criteria**:
- [ ] Разные типы JWT ошибок логируются с разными уровнями
- [ ] Тест: `JwtAuthFilterTest` — каждый тип ошибки проверен

---

### 1.4 Нет ограничений на Telegram long-polling при сбое

**Файл**: `notification/adapter/in/telegram/TelegramBotAdapter.java`

**Проблема**: если `telegramApiClient.getUpdates()` постоянно падает, `@Scheduled(fixedDelay=1000ms)`
бесконечно перезапускается, генерируя тысячи логов ошибок в секунду (log spam + возможный OOM).

**Решение**: добавить счётчик последовательных ошибок и back-off:
```java
private final AtomicInteger consecutiveErrors = new AtomicInteger(0);

@Scheduled(fixedDelayString = "${devcrew.notification.telegram.poll-delay-ms:1000}")
public void pollUpdates() {
  try {
    var updates = telegramApiClient.getUpdates(offset.get());
    consecutiveErrors.set(0);
    // ... process
  } catch (Exception e) {
    int errors = consecutiveErrors.incrementAndGet();
    if (errors == 1 || errors % 10 == 0) {
      log.error("Ошибка Telegram polling ({}й раз подряд)", errors, e);
    }
  }
}
```

**Acceptance Criteria**:
- [ ] Ошибки polling не спамят логи (throttled logging)
- [ ] Тест: `pollUpdates_throttles_log_on_repeated_errors`

---

## Фаза 2 — АРХИТЕКТУРА: Устранение нарушений

### 2.1 TaskParserService в неправильном bounded context

**Файл**: `notification/app/service/TaskParserService.java`

**Проблема**: `TaskParserService` живёт в `notification/`, но парсит задачи через `TaskParserAgent`
(агент). Это логика агентов, не уведомлений. `notification` не должен знать об агентах.

**Решение**: переместить в `agent/app/service/`:
```
notification/app/service/TaskParserService.java
→ agent/app/service/TaskParserService.java
```

`TelegramInboundService` в notification инжектирует `TaskParserService` через его полное имя
(Spring не ограничивает cross-context инъекцию для app-слоя, но это правильнее по смыслу).

**Альтернатива**: создать порт `agent/domain/TaskParsingPort` и реализацию в `agent/app/service/`,
notification инжектирует порт.

**Acceptance Criteria**:
- [ ] `TaskParserService` в `agent/app/service/`
- [ ] `notification` не импортирует `TaskParserService` напрямую, а через порт
- [ ] Все тесты зелёные

---

### 2.2 AgentRole содержит нереализованные роли

**Файл**: `agent/domain/AgentRole.java`

**Проблема**: В `AgentRole` enum есть роли (ORCHESTRATOR, PM, ANALYST), для которых нет агентов
и нет case в `AgentDispatcher`. `AgentDispatcher.dispatch()` бросает
`UnsupportedOperationException` — краш в production при попытке использовать эти роли.

**Решение — вариант A**: удалить нереализованные роли из enum
(YAGNI — не реализовывать то, что не нужно сейчас).

**Решение — вариант B**: реализовать агенты для этих ролей (если они нужны в roadmap).

**Принятое решение**: Вариант A — оставить только 5 реализованных ролей:
`BACKEND_DEV, QA, CODE_REVIEWER, DEVOPS, DOC_WRITER`.

**Дополнительно**: добавить `TASK_PARSER` если нужно (для `TaskParserAgent`).

**Acceptance Criteria**:
- [ ] Все роли в `AgentRole` имеют реализации в `AgentDispatcher`
- [ ] `AgentDispatcher` не содержит unreachable default case
- [ ] `AgentDispatcherTest` покрывает все роли

---

### 2.3 RateLimitPolicy: хрупкое строковое сопоставление

**Файл**: `agent/app/policy/RateLimitPolicy.java`

**Проблема**: определение rate limit через `.getClass().getSimpleName().contains("RateLimit")`
и `e.getMessage().contains("429")` — очень хрупко. Может давать false-positives/negatives
при изменении версии LangChain4j.

**Решение**: использовать специфичный exception type из LangChain4j:
```java
import dev.langchain4j.exception.HttpException;

public boolean isRateLimit(Exception e) {
  if (e instanceof HttpException httpEx) {
    return httpEx.statusCode() == 429;
  }
  // fallback для совместимости
  return e.getMessage() != null && e.getMessage().contains("429");
}
```

Или через cause chain:
```java
private boolean contains429(Throwable t) {
  if (t == null) return false;
  if (t instanceof HttpException h && h.statusCode() == 429) return true;
  return contains429(t.getCause());
}
```

**Acceptance Criteria**:
- [ ] `RateLimitPolicy` использует exception type, не строку
- [ ] Тест с `HttpException(429)` и `HttpException(500)`
- [ ] Старые тесты по-прежнему зелёные

---

### 2.4 CircularAgentPipeline: magic strings

**Файлы**: `agent/app/service/execution/CircularAgentPipeline.java`,
`agent/app/config/AgentProperties.java`

**Проблема**: маркеры `"BUILD SUCCESSFUL"` и `"REQUEST_CHANGES"` — хардкодены в бизнес-логике.
Если агент вернёт другой формат, pipeline молча пропустит.

**Решение A**: вынести в `AgentProperties`:
```yaml
devcrew:
  agent:
    pipeline:
      build-success-marker: "BUILD SUCCESSFUL"
      request-changes-marker: "REQUEST_CHANGES"
```

```java
public class AgentProperties {
  private PipelineProperties pipeline = new PipelineProperties();

  @Data
  public static class PipelineProperties {
    private String buildSuccessMarker = "BUILD SUCCESSFUL";
    private String requestChangesMarker = "REQUEST_CHANGES";
  }
}
```

**Решение B (лучше)**: структурированный вывод — QA возвращает JSON:
```json
{"status": "SUCCESS"|"FAILURE", "details": "...", "failedTests": ["..."]}
```
CodeReview возвращает:
```json
{"decision": "APPROVE"|"REQUEST_CHANGES", "comments": [...]}
```

`CircularAgentPipeline` парсит JSON, не ищет подстроку. Надёжнее, но требует изменения промптов.

**Принятое решение**: Решение A (KISS — не усложнять сейчас) + Javadoc с объяснением.

**Acceptance Criteria**:
- [ ] Маркеры в `AgentProperties.pipeline.*`
- [ ] `CircularAgentPipeline` читает маркеры из properties, не из констант
- [ ] Тест с кастомными маркерами через `new AgentProperties()`

---

### 2.5 BillingPostAgentHook: N+1 запросы

**Файл**: `billing/adapter/in/hook/BillingPostAgentHook.java`

**Проблема**: при каждом вызове `onAgentCompleted()` выполняется:
1. `taskQueryService.getById(taskId)` → SELECT task
2. `organizationQueryService.getProjectById(task.projectId())` → SELECT project
3. `organizationQueryService.getById(project.orgId())` → SELECT org
4. `usageQueryService.countTasksThisMonth(org.id())` → SELECT COUNT

Итого 4 запроса на каждого агента. При CircularAgentPipeline с 20 итерациями = 80 запросов.

**Решение**: изменить сигнатуру `PostAgentHook`:
```java
public interface PostAgentHook {
  void onAgentCompleted(UUID taskId, UUID orgId, UUID projectId, AgentRole role, String result);
}
```

`AgentExecutionService` уже знает `taskId`; нужно прокинуть `projectId` и `orgId` через
`TaskModel` (они уже там должны быть после рефакторинга 1.1).

**Acceptance Criteria**:
- [ ] `PostAgentHook` получает `orgId` и `projectId` как параметры
- [ ] `BillingPostAgentHook` не делает доп. запросов для org/project lookup
- [ ] `AuditPostAgentHook` аналогично использует прокинутые параметры
- [ ] Тесты обновлены под новую сигнатуру

---

## Фаза 3 — КАЧЕСТВО КОДА: DRY / SRP / Идемпотентность

### 3.1 TaskCommandService: дублирование в 6 методах обновления

**Файл**: `task/app/service/command/TaskCommandService.java`

**Проблема**: методы `complete()`, `fail()`, `rateLimited()`, `updateStatus()` и др. каждый
создают `new TaskModel(...)` с 11 полями, меняя лишь 1–2 из них. Нарушение DRY.

**Решение**: добавить метод `withStatus()` в `TaskModel` (record не поддерживает `.with()` нативно,
но можно сделать):
```java
// TaskModel.java
public TaskModel withStatus(TaskStatus newStatus, Instant now) {
  return new TaskModel(
      id, projectId, parentTaskId, title, description, assignedTo,
      newStatus, result, createdAt, now, retryAt);
}

public TaskModel withResult(String newResult, TaskStatus newStatus, Instant now) {
  return new TaskModel(
      id, projectId, parentTaskId, title, description, assignedTo,
      newStatus, newResult, createdAt, now, retryAt);
}

public TaskModel withRetry(Instant retryAt, Instant now) {
  return new TaskModel(
      id, projectId, parentTaskId, title, description, assignedTo,
      TaskStatus.RATE_LIMITED, result, createdAt, now, retryAt);
}
```

`TaskCommandService`:
```java
public void complete(UUID taskId, String result) {
  var task = taskQueryService.getById(taskId);
  taskStore.save(task.withResult(result, TaskStatus.COMPLETED, timeProvider.now()));
}

public void fail(UUID taskId, String reason) {
  var task = taskQueryService.getById(taskId);
  taskStore.save(task.withResult(reason, TaskStatus.FAILED, timeProvider.now()));
}
```

**Acceptance Criteria**:
- [ ] `TaskModel` имеет методы `withStatus()`, `withResult()`, `withRetry()`
- [ ] `TaskCommandService` не содержит 11-field `new TaskModel(...)` конструкторов
- [ ] Все тесты `TaskCommandServiceTest` зелёные
- [ ] `TaskModelTest` — новые тесты для `with*` методов

---

### 3.2 UsageRecordCommandService: нет идемпотентности

**Файл**: `billing/app/service/command/UsageRecordCommandService.java`

**Проблема**: если `PostAgentHook.onAgentCompleted()` вызвался дважды для одного `taskId`
(например, при retry или ошибке), создаётся дублирующая запись биллинга.

**Решение**: добавить `UNIQUE(task_id)` на `usage_records`:
```sql
-- V12__add_unique_task_id_to_usage_records.sql
ALTER TABLE usage_records ADD CONSTRAINT uq_usage_records_task_id UNIQUE (task_id);
```

В `UsageRecordCommandService`:
```java
public void record(UUID taskId, ...) {
  if (usageRecordStore.existsByTaskId(taskId)) {
    log.warn("Дубликат usage record для taskId={}, пропускаем", taskId);
    return;
  }
  // ... create record
}
```

**Acceptance Criteria**:
- [ ] `usage_records.task_id` имеет UNIQUE constraint
- [ ] `UsageRecordCommandService.record()` idempotent
- [ ] Тест: повторный вызов с тем же taskId не создаёт второй записи

---

### 3.3 TokenEstimationPolicy: неточность биллинга

**Файл**: `billing/app/policy/TokenEstimationPolicy.java`

**Проблема**: `estimateTokens(text) = text.length() / 4` — грубое приближение.
Реальное разбиение Claude токенайзера может давать ошибку ±30%. Пользователи могут
переплачивать или недоплачивать.

**Решение краткосрочное**: Оставить char/4 но:
1. Переименовать метод: `estimateTokensApproximate()`
2. Добавить Javadoc с объяснением неточности
3. Добавить конфиг-флаг: `devcrew.billing.chars-per-token: 4` (уже есть в properties)

**Решение долгосрочное** (отдельный план): использовать Anthropic tokenizer (если появится
в Java SDK) или tiktoken-like библиотеку.

**Acceptance Criteria**:
- [ ] Метод переименован в `estimateTokensApproximate`
- [ ] Javadoc объясняет приближение
- [ ] Параметр `chars-per-token` документирован в application.yml

---

### 3.4 AuditPostAgentHook: неструктурированные details

**Файл**: `audit/adapter/in/hook/AuditPostAgentHook.java`

**Проблема**: `details` формируется как строка `"role=%s result=%s".formatted(role, result)`.
Нельзя фильтровать или агрегировать по полям. Результат агента (возможно многосотенный текст)
пишется в `details` целиком.

**Решение**: Создать `AuditDetails` record:
```java
// audit/domain/model/AuditDetails.java
public record AuditDetails(AgentRole role, String status, String resultSummary) {
  public String toJson() {
    return """
        {"role":"%s","status":"%s","summary":"%s"}
        """.formatted(role, status, abbreviate(resultSummary, 200));
  }
  private static String abbreviate(String s, int maxLen) {
    if (s == null || s.length() <= maxLen) return s;
    return s.substring(0, maxLen) + "...";
  }
}
```

Переименовать `details` в `audit_events` → `event_data` (или оставить как TEXT но писать JSON).

**Acceptance Criteria**:
- [ ] `details` содержит JSON, не plain string
- [ ] `result` обрезается до разумного размера (200 символов)
- [ ] `AuditPostAgentHookTest` обновлён

---

### 3.5 AgentOrchestratorImpl: смешанные ответственности

**Файл**: `agent/app/service/AgentOrchestratorImpl.java`

**Проблема**: `AgentOrchestratorImpl` вызывает и `submit()` (создание задачи) и `run()`
(запуск агента). Методы `submit` и `run` можно вызывать независимо.
SRP: два мотива для изменения.

**Решение**: оставить `AgentOrchestratorImpl` как фасад, но убедиться что внутри каждый
метод делегирует в правильный сервис без дублирования логики:
- `submit()` → только `TaskCommandService.create()`
- `run()` → только `AgentExecutionService.execute()`
- Внутренние проверки (`PreRunCheck`) вызываются в `run()`, не в `submit()`

**Acceptance Criteria**:
- [ ] `AgentOrchestratorImpl.submit()` не вызывает никаких агентов
- [ ] `AgentOrchestratorImpl.run()` не создаёт задач
- [ ] Тест: `submit()` без `run()` не запускает агента

---

## Фаза 4 — API: Полнота и производительность

### 4.1 Пагинация на list-эндпоинтах

**Файлы**: контроллеры `task`, `audit`, `billing`

**Проблема**: `GET /api/tasks`, `GET /api/audit`, `GET /api/billing/usage` возвращают
все записи без пагинации. При росте данных — OutOfMemory и медленные ответы.

**Решение**:
```java
@GetMapping
public Page<TaskResponse> list(
    @PageableDefault(size = 20, sort = "createdAt", direction = DESC) Pageable pageable,
    @AuthenticationPrincipal AuthenticatedUser principal) {
  return taskQueryService.getByOrg(principal.orgId(), pageable)
      .map(taskWebMapper::toResponse);
}
```

`TaskStore` добавить:
```java
Page<TaskModel> findByOrgId(UUID orgId, Pageable pageable);
```

**Acceptance Criteria**:
- [ ] `GET /api/tasks?page=0&size=20&sort=createdAt,desc` работает
- [ ] Без параметров: дефолт 20 записей, сортировка по createdAt DESC
- [ ] Тест: ответ содержит поля `content`, `totalElements`, `totalPages`

---

### 4.2 OpenAPI документация

**Проблема**: нет Swagger UI, нет `@Operation` аннотаций. Потребители API не знают
что запросить и что получить.

**Решение**: добавить springdoc-openapi:
```groovy
// build.gradle
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.8'
```

Для каждого контроллера добавить:
```java
@Operation(summary = "Create task", description = "Submit a new task for an AI agent")
@ApiResponses({
  @ApiResponse(responseCode = "201", description = "Task created"),
  @ApiResponse(responseCode = "400", description = "Invalid request"),
  @ApiResponse(responseCode = "403", description = "Plan limit exceeded")
})
```

**Acceptance Criteria**:
- [ ] `/swagger-ui.html` доступен в dev режиме
- [ ] Все 8 контроллеров имеют `@Operation` аннотации
- [ ] Отключить Swagger в prod (`springdoc.api-docs.enabled: false` при `prod` профиле)

---

### 4.3 Получение статуса задачи в реальном времени (SSE)

**Проблема**: клиент (Telegram Bot, IDEA Plugin) должен поллить `GET /api/tasks/{id}`
чтобы узнать когда задача завершена. Это неэффективно.

**Решение**: добавить SSE endpoint:
```java
@GetMapping(value = "/{taskId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter subscribe(@PathVariable UUID taskId) {
  // Emitter получает события через ApplicationEventPublisher
  // TaskCommandService.complete() публикует TaskCompletedEvent
}
```

`AgentExecutionService` публикует `ApplicationEvent` при завершении задачи.
SSE Emitter подписывается и пересылает клиенту.

**Acceptance Criteria**:
- [ ] `GET /api/tasks/{id}/events` — SSE stream статусов
- [ ] При завершении задачи SSE посылает `{"status":"COMPLETED","result":"..."}`
- [ ] Emitter автоматически закрывается по таймауту (30 сек)

---

## Фаза 5 — OBSERVABILITY: Метрики и трейсинг

### 5.1 Метрики для agent tools

**Проблема**: нет метрик для `FileTools`, `GitTools`, `GradleTools`, `DockerTools`.
Невозможно понять сколько времени агент тратит на файловые операции vs. LLM вызовы.

**Решение**: обернуть дорогие операции в `Timer`:
```java
// В каждом Tools-классе добавить MeterRegistry
private final MeterRegistry meterRegistry;

@Tool("Run Gradle tests")
public String runTests(String path) {
  return Timer.builder("devcrew.tool.execution")
      .tag("tool", "gradle")
      .tag("operation", "test")
      .register(meterRegistry)
      .recordCallable(() -> doRunTests(path));
}
```

**Метрики для добавления**:
- `devcrew.tool.execution{tool=gradle/git/file/docker, operation=...}` — Timer
- `devcrew.tool.error{tool=..., operation=...}` — Counter для ошибок
- `devcrew.llm.request{agent_role=...}` — Timer для LLM вызовов

**Acceptance Criteria**:
- [ ] Timer на каждую `@Tool`-операцию в `GradleTools` и `GitTools`
- [ ] Метрики видны в `/actuator/prometheus`
- [ ] Тест с `SimpleMeterRegistry` для каждого tools-класса

---

### 5.2 Structured logging в агентах

**Проблема**: `log.info("Агент завершил задачу: taskId={}", taskId)` — неструктурированный.
В prod с ECS-профилем логи уходят в JSON, но нет полей для фильтрации.

**Решение**: добавить MDC (Mapped Diagnostic Context) на время выполнения задачи:
```java
// AgentExecutionService.execute()
try (var ignored = MDC.putCloseable("taskId", taskId.toString())) {
  MDC.put("agentRole", role.name());
  MDC.put("orgId", orgId.toString());
  // выполнение агента
}
```

В ECS-логах каждое сообщение будет иметь поля `taskId`, `agentRole`, `orgId` —
удобно фильтровать в Kibana/Grafana.

**Acceptance Criteria**:
- [ ] `AgentExecutionService` устанавливает MDC для `taskId`, `agentRole`
- [ ] После завершения MDC очищается (try-with-resources)
- [ ] Тест: MDC корректно снимается при ошибке

---

### 5.3 OpenTelemetry трейсинг

**Проблема**: нет distributed tracing. Невозможно посмотреть полный путь запроса
от Telegram-сообщения до ответа агента.

**Решение**: добавить Spring Boot OTel автоконфигурацию:
```groovy
implementation 'io.micrometer:micrometer-tracing-bridge-otel'
implementation 'io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter'
```

```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% в dev, 0.1 в prod
otel:
  exporter:
    otlp:
      endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4317}
```

**Acceptance Criteria**:
- [ ] `TraceId` и `SpanId` появляются в логах
- [ ] Запрос `POST /api/tasks` создаёт span с дочерним span для агента
- [ ] Отключается при `OTEL_EXPORTER_OTLP_ENDPOINT` не задан

---

## Фаза 6 — ИНФРАСТРУКТУРА: Docker и CI/CD

### 6.1 Dockerfile: JRE не может запускать Gradle тесты

**Файл**: `docker/Dockerfile`

**Проблема**: runtime image — `eclipse-temurin:21-jre-alpine`. JRE не содержит `javac`.
Агенты (`GradleTools.runTests()`) запускают `./gradlew test` в проектах пользователей.
Gradle wrapper скачивает JDK через toolchains — медленно при первом запуске.

**Решение краткосрочное**: переключить на `21-jdk-alpine` (+80 MB но надёжнее):
```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS runtime
```

**Решение долгосрочное**: pre-download JDK toolchain в image:
```dockerfile
RUN ./gradlew --version  # triggers toolchain download
```

**Acceptance Criteria**:
- [ ] Dockerfile использует `eclipse-temurin:21-jdk-alpine` для runtime
- [ ] `docker build` проходит успешно
- [ ] Образ < 600 MB

---

### 6.2 CI: нет шага интеграционных тестов

**Файл**: `.github/workflows/ci.yml`

**Проблема**: CI запускает только unit тесты (`./gradlew test`).
Интеграционные тесты (JPA stores с Testcontainers) не запускаются в CI.
Можно смержить PR с поломанными JPA store тестами.

**Решение**: добавить job `integration-tests`:
```yaml
integration-tests:
  runs-on: ubuntu-latest
  needs: test
  services:
    postgres:
      image: postgres:16-alpine
      env:
        POSTGRES_DB: devcrew
        POSTGRES_USER: devcrew
        POSTGRES_PASSWORD: devcrew
      options: >-
        --health-cmd pg_isready
        --health-interval 10s
  steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
    - run: ./gradlew test -Dspring.profiles.active=tc
```

**Acceptance Criteria**:
- [ ] Integration tests job в `ci.yml`
- [ ] Job запускается после unit tests (needs: test)
- [ ] Fallure в integration tests блокирует merge

---

### 6.3 docker-compose.yml: hardcoded local path

**Файл**: `docker/docker-compose.yml`

**Проблема**: `${PROJECTS_ROOT:-/Users/artemsergienko/IdeaProjects}:/projects` —
хардкоден путь разработчика как дефолт. Другие разработчики получат неправильный путь.

**Решение**: убрать hardcoded дефолт:
```yaml
volumes:
  - ${PROJECTS_ROOT}:/projects
```

Документировать в `.env.example`:
```env
# Путь к директории с проектами для агентов (обязательно)
PROJECTS_ROOT=/your/path/to/projects
```

**Acceptance Criteria**:
- [ ] Нет hardcoded username в `docker-compose.yml`
- [ ] `docker/.env.example` содержит `PROJECTS_ROOT=`
- [ ] `README` или `setup-server.sh` объясняет настройку

---

### 6.4 Добавить health indicator для LLM

**Проблема**: `GET /actuator/health` не проверяет Anthropic API. Deployment считается
healthy, но LLM недоступен — ни один агент не запустится.

**Решение**:
```java
// bootstrap/AnthropicHealthIndicator.java
@Component
public class AnthropicHealthIndicator implements HealthIndicator {
  private final ChatLanguageModel model;

  @Override
  public Health health() {
    try {
      // Минимальный ping запрос
      model.generate("ping");
      return Health.up().build();
    } catch (Exception e) {
      return Health.down().withDetail("error", e.getMessage()).build();
    }
  }
}
```

**Acceptance Criteria**:
- [ ] `/actuator/health` включает `anthropic` indicator
- [ ] При недоступном API ключе — `DOWN`
- [ ] Не вызывается при выключенном `ANTHROPIC_API_KEY`

---

## Фаза 7 — БАЗА ДАННЫХ: Схема и производительность

### 7.1 Добавить org_id в tasks для производительности

**Проблема**: для фильтрации задач по организации нужен JOIN:
`tasks → projects → organizations`. При большом количестве задач — медленно.

**Решение**: денормализация — добавить `org_id` в `tasks`:
```sql
-- V12__add_org_id_to_tasks.sql
ALTER TABLE tasks ADD COLUMN org_id UUID REFERENCES organizations(id);
UPDATE tasks SET org_id = p.org_id FROM projects p WHERE tasks.project_id = p.id;
ALTER TABLE tasks ALTER COLUMN org_id SET NOT NULL;
CREATE INDEX idx_tasks_org_id ON tasks(org_id);
```

`TaskModel` добавить поле `orgId`.
`TaskEntity` добавить поле `orgId`.
Обновить `TaskPersistenceMapper`.

**Acceptance Criteria**:
- [ ] `tasks.org_id` — FK на `organizations`
- [ ] Индекс `idx_tasks_org_id`
- [ ] `TaskStore.findByOrgId()` использует прямой запрос без JOIN

---

### 7.2 Ограничить размер result в tasks

**Проблема**: `tasks.result` — TEXT без ограничений. Агент может записать несколько MB
кода, что замедлит выборки и займёт место в БД.

**Решение**: добавить `result_summary` (VARCHAR 2000) рядом с `result` (TEXT):
```sql
-- V13__add_result_summary_to_tasks.sql
ALTER TABLE tasks ADD COLUMN result_summary VARCHAR(2000);
```

`AgentExecutionService.complete()` заполняет оба поля:
- `result` — полный результат (для детального просмотра)
- `result_summary` — первые 2000 символов (для списка задач)

API `GET /api/tasks` (list) возвращает `resultSummary`, детальный `GET /api/tasks/{id}` — полный `result`.

**Acceptance Criteria**:
- [ ] `tasks.result_summary` — VARCHAR(2000)
- [ ] List endpoint возвращает `resultSummary`
- [ ] Detail endpoint возвращает полный `result`

---

### 7.3 Добавить audit_events.actor_id

**Проблема**: `audit_events` хранит `actor_email` (строку), не ссылку на users.
При смене email истории audit не обновляются. Нет FK.

**Решение**:
```sql
-- V14__add_actor_id_to_audit_events.sql
ALTER TABLE audit_events ADD COLUMN actor_id UUID REFERENCES users(id) ON DELETE SET NULL;
CREATE INDEX idx_audit_events_actor_id ON audit_events(actor_id);
```

`AuditEventModel` добавить `actorId`.
`AuditPostAgentHook` прокидывает `actorId` из `AuthenticatedUser` контекста (через SecurityContextHolder).

**Acceptance Criteria**:
- [ ] `audit_events.actor_id` — FK на users
- [ ] Фильтрация `GET /api/audit?actorId=...` работает
- [ ] `actor_email` оставить для совместимости (не удалять)

---

## Фаза 8 — ТЕСТЫ: Пробелы в покрытии

### 8.1 Тест: изоляция данных между тенантами

**Файл**: новый `task/adapter/in/web/controller/TaskControllerIsolationTest.java`

**Сценарии**:
```java
void getByOrg_returns_only_tasks_of_current_user_org()
void create_associates_task_with_principal_org()
void get_by_id_returns_404_when_task_belongs_to_different_org()
```

---

### 8.2 Тест: JwtAuthFilter с разными типами ошибок

**Файл**: новый `bootstrap/JwtAuthFilterTest.java`

**Сценарии**:
```java
void filter_logs_debug_when_token_expired()
void filter_logs_warn_when_token_malformed()
void filter_passes_valid_token()
void filter_allows_unauthenticated_to_public_endpoint()
```

---

### 8.3 Тест: BillingPostAgentHook идемпотентность

**Файл**: `billing/adapter/in/hook/BillingPostAgentHookTest.java`

**Новый сценарий**:
```java
void onAgentCompleted_second_call_with_same_taskId_does_not_double_charge()
```

---

### 8.4 Тест: CircularAgentPipeline null-safety

**Файл**: `agent/app/service/execution/CircularAgentPipelineTest.java`

**Новые сценарии**:
```java
void execute_handles_null_from_backend_dev_gracefully()
void execute_handles_null_from_qa_gracefully()
```

---

## Итоговая таблица изменений

| # | Файлы | Тип | Приоритет |
|---|-------|-----|-----------|
| 1.1 | `TaskController`, `TaskStore`, `V12` SQL | Security | 🔴 Критично |
| 1.2 | `AuthService`, `OrganizationCreationPort`, новый адаптер | Architecture | 🔴 Критично |
| 1.3 | `JwtAuthFilter` | Security | 🔴 Критично |
| 1.4 | `TelegramBotAdapter` | Reliability | 🟡 Высокий |
| 2.1 | `TaskParserService` → agent module | Architecture | 🟡 Высокий |
| 2.2 | `AgentRole`, `AgentDispatcher` | Correctness | 🟡 Высокий |
| 2.3 | `RateLimitPolicy` | Reliability | 🟡 Высокий |
| 2.4 | `CircularAgentPipeline`, `AgentProperties` | Maintainability | 🟢 Средний |
| 2.5 | `PostAgentHook`, `BillingPostAgentHook`, `AuditPostAgentHook` | Performance | 🟢 Средний |
| 3.1 | `TaskModel`, `TaskCommandService` | Code Quality | 🟢 Средний |
| 3.2 | `UsageRecordCommandService`, `V12` SQL | Correctness | 🟡 Высокий |
| 3.3 | `TokenEstimationPolicy` | Clarity | ⚪ Низкий |
| 3.4 | `AuditPostAgentHook`, `AuditDetails` | Quality | 🟢 Средний |
| 3.5 | `AgentOrchestratorImpl` | SRP | ⚪ Низкий |
| 4.1 | Все контроллеры + `*Store` | Feature | 🟢 Средний |
| 4.2 | `build.gradle`, все контроллеры | Feature | 🟢 Средний |
| 4.3 | Новый SSE endpoint | Feature | ⚪ Низкий |
| 5.1 | `*Tools.java`, `MeterRegistry` | Observability | 🟢 Средний |
| 5.2 | `AgentExecutionService` MDC | Observability | 🟢 Средний |
| 5.3 | `build.gradle`, `application.yml` | Observability | ⚪ Низкий |
| 6.1 | `Dockerfile` | Infrastructure | 🟡 Высокий |
| 6.2 | `.github/workflows/ci.yml` | Infrastructure | 🟡 Высокий |
| 6.3 | `docker-compose.yml` | Infrastructure | 🟢 Средний |
| 6.4 | Новый `AnthropicHealthIndicator` | Reliability | 🟢 Средний |
| 7.1 | SQL migration + `TaskModel/Entity` | Performance | 🟡 Высокий |
| 7.2 | SQL migration + `TaskModel` + API | Performance | 🟢 Средний |
| 7.3 | SQL migration + `AuditEventModel` | Quality | ⚪ Низкий |
| 8.1-8.4 | Новые тесты | Testing | 🟡 Высокий |

---

## Порядок выполнения (рекомендуемый)

1. **Безопасность сначала**: 1.1, 1.2, 1.3 (могут выпускаться отдельным PR)
2. **Критические исправления**: 2.2, 3.2, 6.1, 6.2
3. **Архитектурные**: 2.1, 2.3, 2.4, 2.5
4. **Качество кода**: 3.1, 3.4, 3.5
5. **База данных**: 7.1, 7.2, 7.3
6. **API**: 4.1, 4.2
7. **Observability**: 5.1, 5.2, 5.3, 6.4
8. **Инфраструктура**: 6.3
9. **Тесты**: 8.1, 8.2, 8.3, 8.4

---

## Зависимости

- П10 не зависит от П9 (IntelliJ Plugin)
- Пункты 7.1 + 1.1 связаны (нужно добавить `org_id` в tasks для фильтрации)
- Пункт 2.5 (PostAgentHook) должен быть выполнен до 8.3 (тест идемпотентности)
- Пункт 1.2 (OrganizationCreationPort) требует внимания к ArchUnit (нельзя нарушить правила)

---

## Тест-план (финальная верификация)

```bash
./gradlew spotlessApply
./gradlew test                              # unit + arch tests
./gradlew test -Dspring.profiles.active=tc  # integration tests
./gradlew build                             # полная сборка
```

Ожидаемый результат: BUILD SUCCESSFUL, 280+ тестов (добавятся ~50 новых).
