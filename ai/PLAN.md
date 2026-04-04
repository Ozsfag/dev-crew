# Dev Crew — Plan разработки

## Контекст проекта

Spring Boot 3.5 / Java 21 приложение для оркестрации команды ИИ-агентов.
Архитектор (человек) согласовывает планы; агенты выполняют задачи автономно через LangChain4j + Claude API.

**Группа**: `org.blacksoil` | **Порт**: 8081 | **Пакет**: `org.blacksoil.devcrew`
**Правила архитектуры и стиля**: см. `CLAUDE.md`

---

## Текущее состояние (MVP готов) ✅

### Инфраструктура

- [x] `build.gradle` — Spring Boot 3.5, LangChain4j 0.36.2, ArchUnit, Testcontainers
- [x] `application.yml` + `application-tc.yml` (Testcontainers профиль)
- [x] `V1__init.sql` — таблицы `agents`, `tasks`
- [x] `Dockerfile` (multi-stage) + `docker-compose.yml` (local) + `docker-compose.prod.yml` (server)
- [x] `deploy/` — setup-server.sh, update.sh, sync-repos.sh

### Bounded contexts

- [x] `agent` — AgentModel, AgentStore, AgentQueryService, BackendDevAgent (LangChain4j), AgentExecutionService,
  AgentDispatcher, AgentOrchestratorImpl
- [x] `task` — TaskModel, TaskStore, TaskQueryService, TaskCommandService
- [x] `notification` — NotificationPort, TelegramNotificationAdapter (PostAgentHook)
- [x] `common` — TimeProvider, DomainException, GlobalExceptionHandler

### REST API

- [x] `GET /api/agents`, `GET /api/agents/{id}`
- [x] `POST /api/tasks`, `POST /api/tasks/{id}/run`, `GET /api/tasks/{id}`

### Тесты

- [x] 170 unit-тестов (все зелёные), ArchUnit, @Tag("integration") для JPA-тестов

---

## Roadmap до продакшна (приоритет для продажи)

### Фаза 1 — Надёжность (БЛОКЕР: без этого демо провалится)

#### ~~П1.1 — Async выполнение агентов~~ ✅ DONE

**Проблема**: `POST /api/tasks/{id}/run` ждёт пока агент закончит → HTTP timeout на задачах >30 сек.
**Решение**: Java 21 Virtual Threads + статус-polling.

```
Новый flow:
  POST /api/tasks/{id}/run → 202 Accepted  (запускает агента в VirtualThread)
  GET  /api/tasks/{id}     → { status: "IN_PROGRESS" }  (клиент поллит)
  GET  /api/tasks/{id}     → { status: "COMPLETED", result: "..." }
```

Файлы:

```
agent/app/service/execution/
└── AsyncAgentExecutionService.java   ← @Async + VirtualThread executor
bootstrap/
└── AsyncConfig.java                  ← @EnableAsync + VirtualThreadTaskExecutor
```

Тест: `AsyncAgentExecutionServiceTest` — проверяем что метод возвращает сразу (не блокирует).

---

#### ~~П1.3 — Автовосстановление после rate-limit~~ ✅ DONE

**Проблема**: Anthropic API возвращает HTTP 429/529 при превышении лимита → задача падает в FAILED и теряется.
**Решение**: при rate-limit задача переходит в `RATE_LIMITED` с `retryAt`; планировщик каждые 30 сек повторно запускает задачи, чей `retryAt` наступил.

```
Новый flow:
  execute() → HTTP 429 от LLM
  → RateLimitPolicy.isRateLimit(e) = true
  → TaskCommandService.rateLimited(taskId, retryAt)   // статус RATE_LIMITED
  → RateLimitRetryScheduler (каждые 30с)
      findRateLimitedReadyToRetry(now) → [task]
      AgentOrchestrator.run(taskId, role)              // повторный запуск
```

Файлы:

```
agent/app/config/
└── RateLimitProperties.java        ← devcrew.agent.rate-limit.retry-delay (default 60s)
agent/app/policy/
└── RateLimitPolicy.java            ← isRateLimit(Throwable), retryAt(Instant)
agent/bootstrap/
└── RateLimitRetryScheduler.java    ← @Scheduled(fixedDelayString=...) каждые 30с
task/domain/
├── TaskStatus.java                 ← + RATE_LIMITED
└── TaskModel.java                  ← + retryAt: Instant
db/migration/
└── V9__add_retry_at_to_tasks.sql   ← retry_at TIMESTAMPTZ
```

Тесты: `RateLimitPolicyTest` (10), `RateLimitRetrySchedulerTest` (2), `AgentExecutionServiceTest` (+3), `TaskCommandServiceTest` (+2).

---

#### ~~П1.2 — Защита FileTools от path traversal~~ ✅ DONE

**Проблема**: агент может написать `readFile("/etc/passwd")` или `writeFile("/bin/bash", "...")`.
**Решение**: `SandboxPolicy` — разрешать только пути внутри `/projects/`.

```
agent/app/policy/
└── SandboxPolicy.java    ← validatePath(String path): void — бросает если вне /projects/
agent/adapter/out/llm/tools/
└── FileTools.java        ← вызывает SandboxPolicy перед каждой операцией
agent/app/config/
└── SandboxProperties.java ← devcrew.sandbox.root = /projects
```

Тест: `SandboxPolicyTest` — путь внутри → OK, путь снаружи или `../` → exception.

---

### Фаза 2 — Безопасность (нужна для первого показа компании)

#### ~~П2.1 — JWT аутентификация~~ ✅ DONE

```
auth/ (новый bounded context)
├── domain/
│   ├── UserModel.java          ← id, email, role (ARCHITECT | VIEWER)
│   └── UserStore.java
├── app/service/
│   ├── AuthService.java        ← login(email, password) → TokenPair
│   └── TokenService.java       ← generate/validate JWT
├── adapter/in/web/
│   └── AuthController.java     ← POST /api/auth/login, POST /api/auth/refresh
└── adapter/out/persistence/
    └── UserJpaStore.java
bootstrap/
└── SecurityConfig.java         ← Spring Security + JWT filter
```

Зависимость: `spring-boot-starter-security` + `jjwt`.
Миграция: `V2__users.sql`.

---

#### ~~П2.2 — Audit log~~ ✅ DONE

**Зачем**: компании нужно знать кто запустил какого агента и что изменилось.

```
audit/ (новый bounded context)
├── domain/AuditEventModel.java  ← id, actorEmail, action, entityId, details, timestamp
├── domain/AuditStore.java
└── adapter/out/persistence/
    └── AuditJpaStore.java

Реализует PostAgentHook → пишет запись при каждом завершении агента.
Миграция: V3__audit_events.sql
```

API: `GET /api/audit?from=...&to=...` — история действий.

---

### Фаза 3 — Полная команда агентов

#### ~~П3.1 — GitTools~~ ✅ DONE

```
agent/adapter/out/llm/tools/
└── GitTools.java
    @Tool gitStatus(projectPath)
    @Tool gitAdd(projectPath, files)
    @Tool gitCommit(projectPath, message)
    @Tool gitPush(projectPath, branch)
    @Tool createBranch(projectPath, branchName)
    @Tool getCurrentBranch(projectPath)
```

---

#### ~~П3.2 — QaAgent~~ ✅ DONE

System prompt: `prompts/qa.md`
Tools: FileTools, GradleTools (runTests, checkCoverage)
Задача: получает путь к модулю → пишет тесты → прогоняет → репортит покрытие.

---

#### ~~П3.3 — CodeReviewAgent~~ ✅ DONE

System prompt: `prompts/code-review.md`
Tools: FileTools, GitTools (gitDiff)
Задача: получает PR diff → проверяет по чеклисту (архитектура, безопасность, N+1) → возвращает review.

---

#### ~~П3.4 — DevOpsAgent~~ ✅ DONE

System prompt: `prompts/devops.md`
Tools: FileTools, CommandRunner (docker build/push), GitTools
Задача: обновляет docker-compose, Dockerfile, CI/CD конфиги.

---

#### ~~П3.5 — DocWriterAgent~~ ✅ DONE

System prompt: `prompts/doc-writer.md`
Tools: FileTools, GitTools
Задача: документирует код — Javadoc для публичных классов и методов, README-секции, OpenAPI-аннотации.
Диспетчеризация: `AgentDispatcher` маршрутизирует `AgentRole.DOC_WRITER` → `DocWriterAgent`.

---

### Фаза 4 — Мультитенантность (нужна для SaaS/продажи нескольким компаниям)

```
organization/ (новый bounded context)
├── OrganizationModel.java   ← id, name, plan (FREE | PRO | ENTERPRISE)
└── ProjectModel.java        ← id, orgId, name, repoPath

Все TaskModel, AuditEventModel привязаны к projectId.
Row-level isolation: каждый запрос фильтруется по orgId из JWT.

Миграция: V4__organizations.sql, V5__add_project_id_to_tasks.sql
```

---

### Фаза 5 — Observability

```yaml
# Добавить в build.gradle:
implementation 'io.micrometer:micrometer-registry-prometheus'
implementation 'io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter'

  # Метрики:
devcrew_task_total{status, role}         ← сколько задач по статусам/ролям
devcrew_agent_duration_seconds{role}     ← время работы агента
devcrew_llm_tokens_total{role}           ← потребление токенов

  # Структурированные логи:
logback-spring.xml → JSON format → ELK / Loki
```

Docker Compose добавить: Prometheus + Grafana сервисы.

---

### Фаза 6 — Монетизация

```
billing/ (новый bounded context)
├── UsageRecordModel.java    ← taskId, orgId, tokensUsed, costUsd, timestamp
├── PlanModel.java           ← FREE(50 tasks/mo), PRO(unlimited, $199/mo)
└── adapter/out/stripe/
    └── StripeWebhookAdapter.java

API: GET /api/billing/usage — текущий расход
LangChain4j TokenUsage → UsageRecordCommandService → monthly summary
```

---

### Фаза 7 — Устранение пробелов (по результатам код-ревью)

Ревью проведено 2026-04-04. Архитектура, стиль и основные паттерны соответствуют CLAUDE.md.
Найденные пробелы зафиксированы ниже.

#### П7.1 — Тесты для `organization/` контекста

**Проблема**: контекст `organization/` не имеет ни одного unit-теста — ~12 публичных методов без
покрытия при требовании 100% (CLAUDE.md).

```
Создать:
organization/app/service/command/OrganizationCommandServiceTest.java  ← unit, Mockito
organization/app/service/query/OrganizationQueryServiceTest.java      ← unit, Mockito
organization/adapter/in/web/controller/OrganizationControllerTest.java ← standalone MockMvc
```

Именование: `method_scenario_expectedBehavior` (create_saves_org_with_free_plan и т.п.)

---

#### П7.2 — Тесты для `audit/` service-слоя

**Проблема**: `AuditPostAgentHook` протестирован, но `AuditCommandService` и `AuditQueryService`
покрытия не имеют.

```
Создать:
audit/app/service/command/AuditCommandServiceTest.java  ← unit, Mockito
audit/app/service/query/AuditQueryServiceTest.java      ← unit, Mockito
```

---

#### П7.3 — Stripe: реализация вместо stub

**Проблема**: `StripeWebhookAdapter` содержит два TODO и фактически игнорирует все входящие
вебхуки без верификации подписи.

```
Реализовать:
  1. Верификацию Stripe-Signature header (HMAC-SHA256)
  2. Обработку customer.subscription.updated / deleted → OrganizationCommandService.updatePlan()
  3. StripeWebhookAdapterTest — unit-тест с mock-событиями

Новые свойства:
  billing/app/config/StripeProperties.java ← devcrew.billing.stripe.webhook-secret
```

---

#### П7.4 — Фиксация `Instant.now()` в тест-хелперах

**Проблема**: ~50 мест в интеграционных тестах используют `Instant.now()` для создания тестовых
данных вместо фиксированной константы — тесты зависят от системного времени.

```
Исправить в:
  audit/adapter/out/persistence/store/AuditJpaStoreTest.java
  billing/adapter/out/persistence/store/UsageRecordJpaStoreTest.java
  (и других интеграционных тестах по результатам grep)

Правило: Instant.parse("2026-01-01T10:00:00Z") — фиксированная константа в хелпере,
не Instant.now() и не Instant.now().minusSeconds(N).
```

---

## Текущий приоритет выполнения

```
✅ П1.1  Async выполнение агентов
✅ П1.2  SandboxPolicy для FileTools
✅ П1.3  Автовосстановление после rate-limit
✅ П2.1  JWT аутентификация
✅ П2.2  Audit log
✅ П3.1  GitTools
✅ П3.2  QaAgent
✅ П3.3  CodeReviewAgent
✅ П3.4  DevOpsAgent
✅ П3.5  DocWriterAgent
✅ П4    Мультитенантность
✅ П5    Observability (Prometheus + Grafana)
✅ П6    Монетизация (Stripe)
   П7.1  Тесты organization/ контекста
   П7.2  Тесты audit/ service-слоя
   П7.3  Stripe: верификация + обработка webhook
   П7.4  Фиксация Instant.now() в тест-хелперах
```

---

## Напоминания по стилю (из CLAUDE.md)

- `ArgumentCaptor` → `ArgumentCaptor.captor()` (не `forClass()`) — Mockito 5
- Комментарии в коде — на русском
- `@Transactional` — только в `app/service/**` и `adapter/out/**`, никогда в контроллерах
- `@BeforeEach` вместо `@InjectMocks` когда сервис принимает `List<Hook>`
- `Mappers.getMapper(Foo.class)` в standalone MockMvc тестах (не `new Foo()`)
- MapStruct mapper: `@Mapper(componentModel = "spring")`
- Enum в БД: `@Enumerated(EnumType.STRING)`
- Новое поле/таблица = новый Flyway-скрипт `V{n}__*.sql`
- Integration тесты помечать `@Tag("integration")` — не запускаются без Docker
