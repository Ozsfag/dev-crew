# CLAUDE.md — Dev Crew

## Что это за проект

Spring Boot 3.5 / Java 21 приложение. Оркестрирует команду ИИ-агентов для разработки программных проектов.
Архитектор (человек) согласовывает планы; агенты выполняют задачи автономно через LangChain4j + Claude API.

**Группа**: `org.blacksoil` | **Порт**: 8081 | **БД**: PostgreSQL + Flyway

---

## Структура корневого каталога

В корне проекта обязательно присутствуют две папки:

```
dev-crew/
├── ai/        ← системные промпты агентов, конфиги LangChain4j, шаблоны задач
├── docker/    ← Dockerfile, docker-compose файлы, конфиги окружений
└── src/
```

**`ai/`** — всё, что относится к поведению ИИ-агентов. Структура:

```
ai/
├── prompts/
│   ├── backend-dev.md    ← системный промпт BackendDevAgent
│   ├── qa.md             ← системный промпт QaAgent
│   ├── code-review.md    ← системный промпт CodeReviewAgent
│   ├── devops.md         ← системный промпт DevOpsAgent
│   └── doc-writer.md     ← системный промпт DocWriterAgent
└── templates/            ← шаблоны задач, few-shot примеры
```

Промпты загружаются через LangChain4j `@SystemMessage(fromResource = "prompts/*.txt")` — `ai/`
добавлен в `sourceSets.main.resources.srcDirs` в `build.gradle`, поэтому файлы доступны по
classpath-пути `prompts/*.txt` без дублирования в `src/main/resources`.

**`docker/`** — инфраструктурные файлы:

```
docker/
├── Dockerfile
├── docker-compose.yml
└── docker-compose.prod.yml
```

Docker-файлы не держать в корне проекта.

---

## Архитектура: Hexagonal (Ports & Adapters)

```
domain/          ← pure records, port-interfaces, exceptions. Без Spring.
app/             ← @Service, оркестраторы, бизнес-логика, config/properties
adapter/in/      ← REST-контроллеры (@RestController)
adapter/out/     ← JPA-stores, HTTP-клиенты, LangChain4j-адаптеры, persistence-маперы
bootstrap/       ← Spring config-beans, scheduling
common/          ← исключения, observability, TimeProvider, GlobalExceptionHandler
```

**Правило зависимостей**: `adapter → app → domain`. Ни один слой не смотрит "вверх".
ArchUnit-тест закрепляет это правило в коде — нарушение = красный тест.

---

## Bounded contexts (пакеты)

| Пакет          | Назначение                                            |
|----------------|-------------------------------------------------------|
| `agent`        | Определения агентов, роли, состояние, оркестрация     |
| `task`         | Задачи для агентов: создание, декомпозиция, статусы   |
| `auth`         | JWT-аутентификация, пользователи, refresh-токены      |
| `audit`        | Аудит действий агентов и пользователей                |
| `organization` | Организации и проекты, к которым привязаны задачи     |
| `notification` | Уведомления архитектору (Telegram)                    |
| `billing`      | Учёт использования, лимиты планов, Stripe webhook     |
| `common`       | Shared: exceptions, TimeProvider, web error handling  |

---

## Структура подпакетов: agent (эталон)

```
agent/
├── app/
│   ├── config/
│   │   ├── AgentConfig.java             ← @EnableConfigurationProperties
│   │   └── AgentProperties.java         ← devcrew.agent.*
│   └── service/
│       ├── execution/   ← AgentExecutionService, AgentDispatcher
│       ├── query/       ← AgentQueryService
│       └── AgentOrchestratorImpl.java   ← оркестратор на верхнем уровне
├── bootstrap/
│   ├── LangChain4jAgentConfig.java  ← создаёт бины LangChain4j-агентов (зависит от adapter/out)
│   └── RateLimitRetryScheduler.java ← @Scheduled-планировщик
└── domain/
    ├── model/
    │    └── AgentModel.java 
    ├── agent/
    │    ├── BackendDevAgent.java
    │    ├── QaAgent.java
    │    ├── CodeReviewAgent.java
    │    ├── DevOpsAgent.java
    │    └── DocWriterAgent.java
    ├── hook/
    │    └── PostAgentHook.java
    ...
```

`bootstrap/` внутри bounded context — допустимо, когда конфигурационный бин создаёт объекты из
`adapter/out/` (нарушало бы правило `app → domain`, если разместить в `app/config/`). В таких
случаях Spring-бин переносится в `<context>/bootstrap/`, который может зависеть на любой слой.

**Правило**: сервисы группируются по ответственности в подпакеты. Плоский `app/service/` с 5+ классами — запрещён.

**Правило domain/**: при 5+ классах группировать по типу в подпакеты:

| Подпакет        | Что содержит                                        |
|-----------------|-----------------------------------------------------|
| `model/`        | `*Model` — immutable records (доменные объекты)     |
| `store/`        | `*Store` — port-интерфейсы персистентности          |
| `agent/`        | `*Agent` — LangChain4j AiService-интерфейсы         |
| `hook/`         | `*Hook` — extension points после завершения         |
| `check/`        | `*Check` — extension points перед выполнением       |
| _(корень)_      | `*Orchestrator`, `*Exception`, enums                |

### Структура подпакетов: notification

`notification` — намеренно упрощённый контекст без `app/service/` (адаптер реализует порт напрямую):

```
notification/
├── adapter/out/telegram/
│   ├── TelegramApiClient.java         ← внутренний интерфейс (не domain-порт)
│   ├── TelegramApiClientImpl.java     ← реализация через Spring RestClient
│   └── TelegramNotificationAdapter.java ← реализует NotificationPort + PostAgentHook
├── app/config/
│   ├── TelegramConfig.java            ← @EnableConfigurationProperties
│   └── TelegramProperties.java        ← devcrew.notification.telegram.*
├── bootstrap/
│   └── TelegramClientConfig.java      ← @Bean TelegramApiClient (зависит от adapter/out)
└── domain/
    └── NotificationPort.java          ← port-интерфейс
```

---

## Структура пакетов: расположение файлов

Каждый тип файла живёт в строго определённом подпакете. Пример для модуля `organization`:

```
organization/
├── adapter/
│   ├── in/web/
│   │   ├── controller/   ← OrganizationController.java       (@RestController)
│   │   ├── mapper/       ← OrganizationWebMapper.java        (*WebMapper)
│   │   └── dto/          ← CreateOrganizationRequest.java    (*Request / *Response)
│   └── out/persistence/
│       ├── entity/       ← OrganizationEntity.java           (@Entity)
│       ├── mapper/       ← OrganizationPersistenceMapper.java (*PersistenceMapper)
│       ├── repository/   ← OrganizationRepository.java       (Spring Data JpaRepository)
│       └── store/        ← OrganizationJpaStore.java         (*JpaStore — реализация порта)
├── app/
│   ├── config/           ← OrganizationConfig.java, OrganizationProperties.java
│   └── service/
│       ├── command/      ← OrganizationCommandService.java
│       └── query/        ← OrganizationQueryService.java
└── domain/
    ├── model/            ← OrganizationModel.java
    └── store/            ← OrganizationStore.java (port-интерфейс)
```

| Тип                                                                  | Подпакет                              |
|----------------------------------------------------------------------|---------------------------------------|
| `@RestController`                                                    | `adapter/in/web/controller/`          |
| `@Mapper / *WebMapper` (MapStruct)                                   | `adapter/in/web/mapper/`              |
| `*Request` / `*Response`                                             | `adapter/in/web/dto/`                 |
| `@Entity`                                                            | `adapter/out/persistence/entity/`     |
| `@Mapper / *PersistenceMapper` (MapStruct)                           | `adapter/out/persistence/mapper/`     |
| `@Repository / *Repository` (Spring Data)                            | `adapter/out/persistence/repository/` |
| `*JpaStore` (реализация порта)                                       | `adapter/out/persistence/store/`      |
| `*Model` (records)                          | `domain/model/`   |
| `*Store` (port-интерфейс)                   | `domain/store/`   |
| `*Agent` (LangChain4j AiService-интерфейс)  | `domain/agent/`   |
| `*Hook` (extension point after action)      | `domain/hook/`    |
| `*Check` (extension point before action)    | `domain/check/`   |
| `*Orchestrator`, `*Exception`, enums        | `domain/` (корень)|

**Запрещено**: класть в одну папку разнородные типы (например, `Entity` + `Repository` + `JpaStore` в одном плоском
`persistence/`; `*Model` + `*Store` + `*Hook` в плоском `domain/` при 5+ классах).

---

## Соглашения по именованию

| Суффикс                  | Что это                                             |
|--------------------------|-----------------------------------------------------|
| `*Model`                 | Immutable record в domain                           |
| `*Entity`                | JPA-сущность в adapter/out                          |
| `*Dto`                   | DTO в app-слое                                      |
| `*Request` / `*Response` | DTO в web-слое                                      |
| `*Store`                 | Port-интерфейс (domain)                             |
| `*JpaStore`              | Реализация Store (adapter/out)                      |
| `*Repository`            | Spring Data JPA interface                           |
| `*Orchestrator`          | Координирует несколько сервисов                     |
| `*Mapper`                | MapStruct-интерфейс (componentModel = "spring")     |
| `*Factory`               | Конструирует domain-объекты                         |
| `*Hook`                  | Extension point after action (OCP-паттерн)          |
| `*Check`                 | Extension point before action (OCP-паттерн)         |
| `*Properties`            | `@ConfigurationProperties` класс                    |
| `*Config`                | `@Configuration` + `@EnableConfigurationProperties` |
| `*Policy`                | Стратегия/правило                                   |
| `*Agent`                 | LangChain4j AiService-интерфейс агента              |

---

## Конфигурация: ConfigurationProperties

```java
// app/config/AgentProperties.java
@Data
@ConfigurationProperties(prefix = "devcrew.agent")
public class AgentProperties {
    private String model = "claude-sonnet-4-6";
    private int maxTokens = 8096;
    private int maxIterations = 20;
}

// app/config/AgentConfig.java
@Configuration
@EnableConfigurationProperties(AgentProperties.class)
public class AgentConfig {
}
```

**application.yml** — все свойства объявлены явно, даже если равны дефолту:

```yaml
devcrew:
  agent:
    model: claude-sonnet-4-6
    max-tokens: 8096
    max-iterations: 20
  notification:
    telegram:
      enabled: false
      bot-token: ${TELEGRAM_BOT_TOKEN:}
      chat-id: ${TELEGRAM_CHAT_ID:}
```

**Запрещено**: хардкодить модели, токены и флаги прямо в `@Service` / `@Component`.

---

## LangChain4j паттерны

### AiService — декларативный агент

```java
// domain-порт агента
public interface BackendDevAgent {
    String execute(String task);
}

// bootstrap — регистрация через LangChain4j
@Bean
public BackendDevAgent backendDevAgent(ChatLanguageModel model) {
    return AiServices.builder(BackendDevAgent.class)
            .chatLanguageModel(model)
            .tools(fileTools, gradleTools, gitTools)
            .build();
}
```

### Tools — инструменты агента

```java
// adapter/out/tool/
public class GradleTools {
    @Tool("Run Gradle tests and return output")
    public String runTests(String projectPath) { ...}

    @Tool("Build the Gradle project")
    public String buildProject(String projectPath) { ...}
}
```

### PostAgentHook — расширение без модификации

```java
// Agent-модуль объявляет интерфейс:
public interface PostAgentHook {
    void onAgentCompleted(UUID taskId, AgentRole role, String result);
}
// Notification реализует. Agent не знает о Notification.
```

---

## Ключевые паттерны

### Маппинг в три слоя

```
domain Model ←→ PersistenceMapper ←→ Entity
domain Model ←→ AppMapper         ←→ AppDto
AppDto       ←→ WebMapper         ←→ Request/Response
```

### Транзакции

- `@Transactional` — только на `app/service/**` и `adapter/out/.../store/**`
- `@Transactional(readOnly = true)` — на query-сервисах
- В контроллерах — **никогда**

### Тестируемость времени

Используй `TimeProvider` вместо `Instant.now()` / `LocalDate.now()` напрямую.

---

## Принципы SOLID

Код строится в соответствии с SOLID на трёх уровнях: классы, компоненты и зависимости между компонентами.

### Уровень 1: Классы

| Принцип | Правило                                       | Пример в проекте                                                                                                                                   |
|---------|-----------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| **SRP** | Один мотив для изменения                      | `AgentExecutionService` выполняет задачи; `AgentQueryService` читает статус — разные классы                                                        |
| **OCP** | Открыт для расширения, закрыт для модификации | `PostAgentHook` — новые реакции на завершение агента без правки ядра                                                                               |
| **LSP** | Подтип подставляем везде, где ожидается тип   | `AgentJpaStore` реализует `AgentStore` — любой сервис, зависящий от `AgentStore`, работает с любой реализацией                                     |
| **ISP** | Узкие интерфейсы вместо «God-интерфейса»      | Отдельные `*Store`, `*QueryPort` с минимальным контрактом; никакой лишней зависимости                                                              |
| **DIP** | Зависимость от абстракций, не конкреций       | Сервисы получают `AgentStore`, а не `AgentJpaStore`; LangChain4j-агенты инжектируются как domain-интерфейс (`BackendDevAgent`), не как `AiService` |

### Уровень 2: Компоненты (bounded contexts)

- **SRP**: каждый context (`agent`, `task`, `auth`, `audit`, `notification`, `billing`) отвечает за одну область. Изменение в
  `audit` не затрагивает `agent`.
- **OCP**: новые поведения добавляются через порты (`PostAgentHook`), а не через изменение существующих сервисов.
- **Stable Abstractions**: `domain/` — только интерфейсы и records (стабильные, почти не меняются). `adapter/out/` —
  конкретные реализации (нестабильные, меняются чаще).

### Уровень 3: Зависимости между компонентами

```
notification → agent → domain
audit        → agent → domain
billing      → agent → domain   (PostAgentHook, PreRunCheck)
billing      → task             (получает task по taskId в BillingPostAgentHook)
billing      → organization     (получает OrgPlan для проверки лимита)
          (стрелка = "зависит от")
```

- **Acyclic Dependencies Principle (ADP)**: нет циклов между контекстами. `notification` знает об `agent` через
  `PostAgentHook`; `agent` не знает о `notification`. `billing` использует порты `agent` и читает `task`/`organization`,
  но ни один из них не знает о `billing`. Циклы = запрещены.
- **Stable Dependencies Principle (SDP)**: нестабильные модули (adapters, конкретные context-ы) зависят от стабильных (
  domain-интерфейсы). Нарушение фиксирует ArchUnit — красный тест.
- **Stable Abstractions Principle (SAP)**: самые стабильные пакеты — самые абстрактные. `domain/` никогда не зависит от
  Spring или JPA. `adapter/out/` зависит от всего, но от него не зависит никто внутри проекта.

**Запрещено**:

- **НЕ** создавать циклические зависимости между bounded contexts
- **НЕ** делать один класс/сервис ответственным за несколько несвязанных областей
- **НЕ** объявлять «God-интерфейс» со всеми возможными методами — дробить на узкие порты
- **НЕ** зависеть от конкретных реализаций (`*JpaStore`, `*AiService`) — только от абстракций (`*Store`,
  domain-интерфейсов)

---

## Принципы KISS / DRY / YAGNI

### KISS — Keep It Simple

- Простейшее решение, которое работает — предпочтительнее «умного»
- Метод длиннее ~20 строк — сигнал, что он делает слишком много; разбить
- Не обобщать до абстракции, пока нет второго конкретного случая

### DRY — Don't Repeat Yourself

- Повторяющаяся бизнес-логика → общий метод или сервис
- Повторяющаяся конфигурация → `*Properties`
- **Исключение**: дублирование в тестах допустимо — читаемость теста важнее DRY

### YAGNI — You Ain't Gonna Need It

- Реализовывать только то, что нужно прямо сейчас
- Не добавлять поля, флаги, параметры «на будущее» без конкретной задачи в backlog
- Не проектировать под гипотетические сценарии, которых нет в требованиях

**Запрещено**:

- **НЕ** добавлять `enabled: boolean` / `version: int` без явной необходимости (YAGNI)
- **НЕ** создавать интерфейс, если есть только одна реализация и вторая не планируется — кроме port-интерфейсов в
  `domain/` (YAGNI + KISS)
- **НЕ** копировать блок кода в третий раз без выноса в метод (DRY)
- **НЕ** писать «универсальный» обработчик, когда нужен конкретный (KISS)

---

## Тестирование

### Типы тестов

| Тип                     | Аннотации                                                      | Контекст Spring |
|-------------------------|----------------------------------------------------------------|-----------------|
| Unit                    | `@ExtendWith(MockitoExtension.class)` + AssertJ                | ❌ нет           |
| Controller (standalone) | `@ExtendWith(MockitoExtension.class)` + `MockMvcBuilders`      | ❌ нет           |
| Integration (JPA store) | `@SpringBootTest` + `@ActiveProfiles("tc")` + `@Transactional` | ✅ полный        |

### Покрытие кода — 100%

Каждая строка production-кода должна быть покрыта тестами. 100% — не рекомендация, а требование.

Для каждого публичного метода обязательны:

| Сценарий                   | Пример теста                                              |
|----------------------------|-----------------------------------------------------------|
| Happy path                 | `rateLimited_sets_rate_limited_status_and_retryAt`        |
| Ошибка / not found         | `getById_throws_not_found_when_missing`                   |
| Каждая ветка `if` / `else` | `isRateLimit_returns_false_for_generic_exception`         |
| Каждый `case` в `switch`   | по одному тесту на каждую `AgentRole` в `dispatchToAgent` |
| Граничные значения         | пустой список, `null`-поля, нулевые числа                 |

**Правило полноты**: каждый новый `case` в `switch` — новый тест; каждый новый `catch`-блок — новый тест, который его провоцирует; новый статус в `enum` — новый тест для ветки, которая его обрабатывает.

```java
// AgentDispatcherTest — каждый case покрыт отдельно:
void dispatch_BACKEND_DEV_calls_backendDevAgent()
void dispatch_QA_calls_qaAgent()
void dispatch_CODE_REVIEWER_calls_codeReviewAgent()
void dispatch_DEVOPS_calls_devOpsAgent()
void dispatch_DOC_WRITER_calls_docWriterAgent()
void dispatch_unsupported_role_throws_UnsupportedOperationException()

// два catch-пути в AgentExecutionService.execute():
void execute_fails_task_when_dispatcher_throws()
void execute_marks_task_rate_limited_when_llm_returns_429()
```

---

### Расположение тестов

Тестовый файл живёт в **том же подпакете**, что и тестируемый класс:

```
src/main/java/.../task/app/service/command/TaskCommandService.java
src/test/java/.../task/app/service/command/TaskCommandServiceTest.java   ← ✅

src/test/java/.../task/                                                  ← ❌ неправильно
```

### Именование тестов

Формат: `метод_сценарий_ожидаемоеПоведение`

```java
void create_saves_task_with_pending_status()
void execute_fails_task_when_agent_throws()
void getById_throws_not_found_when_missing()
void isRateLimit_returns_true_for_429_in_message()
```

### Unit-тесты (Mockito)

```java
@ExtendWith(MockitoExtension.class)
class TaskCommandServiceTest {

  @Mock private TaskStore taskStore;
  @Mock private TimeProvider timeProvider;

  // @InjectMocks — когда все зависимости — одиночные моки
  @InjectMocks private TaskCommandService taskCommandService;

  @Test
  void create_saves_task_with_pending_status() {
    var now = Instant.parse("2026-01-01T10:00:00Z");
    when(timeProvider.now()).thenReturn(now);
    when(taskStore.save(any())).thenAnswer(inv -> inv.getArgument(0));  // возвращаем то, что сохранили

    taskCommandService.create("Write tests", "TDD for module X", AgentRole.QA, null, null);

    var captor = ArgumentCaptor.<TaskModel>captor();  // ← captor(), не forClass()
    verify(taskStore).save(captor.capture());
    assertThat(captor.getValue().status()).isEqualTo(TaskStatus.PENDING);
  }
}
```

**Когда нельзя использовать `@InjectMocks`**: если сервис принимает `List<Hook>` или другой generic-тип — Mockito не умеет инжектировать коллекции. Создавать вручную в `@BeforeEach`:

```java
@BeforeEach
void setUp() {
  agentExecutionService =
      new AgentExecutionService(
          agentDispatcher,              // ← маршрутизатор к агентам
          taskQueryService,
          taskCommandService,
          List.of(postAgentHook),       // ← List собираем вручную
          new SimpleMeterRegistry(),
          rateLimitPolicy,
          timeProvider);
}
```

**`@ConfigurationProperties`-классы** тестировать через `new FooProperties()` — дефолты совпадают с `application.yml`:

```java
var properties = new RateLimitProperties();  // default retry-delay = 60s
var policy = new RateLimitPolicy(properties);
```

### Тесты контроллеров (standalone MockMvc)

Без `@SpringBootTest` — поднимаем только нужные бины вручную:

```java
@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

  @Mock private TaskQueryService taskQueryService;
  @Mock private AgentOrchestrator agentOrchestrator;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    var controller = new TaskController(
        taskQueryService,
        agentOrchestrator,
        Mappers.getMapper(TaskWebMapper.class));  // ← MapStruct: Mappers.getMapper(), не new/mock
    mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new GlobalExceptionHandler())  // ← подключаем обработчик ошибок
        .build();
  }

  @Test
  void GET_tasks_id_returns_404_when_missing() throws Exception {
    var id = UUID.randomUUID();
    when(taskQueryService.getById(id)).thenThrow(new NotFoundException("Task", id));

    mockMvc.perform(get("/api/tasks/{id}", id))
        .andExpect(status().isNotFound());
  }
}
```

Правило именования тестов контроллеров: `МЕТОД_path_ожидаемоеПоведение`:
```java
void POST_tasks_returns_201_with_task_id()
void GET_tasks_id_returns_404_when_missing()
void POST_tasks_returns_400_when_title_missing()
```

### Тесты с метриками (Micrometer)

Вместо `@SpringBootTest` — `new SimpleMeterRegistry()`:

```java
private SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

@Test
void execute_increments_COMPLETED_counter_on_success() {
  // ... вызов метода ...

  var counter = meterRegistry
      .find("devcrew.task.total")
      .tag("status", "COMPLETED")
      .tag("role", AgentRole.BACKEND_DEV.name())
      .counter();
  assertThat(counter).isNotNull();
  assertThat(counter.count()).isEqualTo(1.0);
}
```

### Интеграционные тесты (JPA + Testcontainers)

```java
// src/test/java/.../common/IntegrationTestBase.java
@SpringBootTest
@ActiveProfiles("tc")
@Transactional
@Tag("integration")
public abstract class IntegrationTestBase {}

// конкретный тест — только то, что уникально
class TaskJpaStoreTest extends IntegrationTestBase {

  @Autowired private TaskJpaStore taskJpaStore;

  @Test
  void save_and_findById_roundtrip() {
    var saved = taskJpaStore.save(taskModel());
    var found = taskJpaStore.findById(saved.id());
    assertThat(found).isPresent();
  }
}
```

Запускаются только явно:
```bash
./gradlew test -Dspring.profiles.active=tc
```

### Тестовые данные

Вспомогательные методы для создания объектов — в конце класса, `private`:

```java
private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

private TaskModel taskModel(UUID id, TaskStatus status) {
  return new TaskModel(
      id, null, null, "title", "description",
      AgentRole.BACKEND_DEV, status, null,
      NOW, NOW, null);
}
```

Использовать фиксированный `Instant.parse(...)` — не `Instant.now()` (тест не должен зависеть от
системного времени). `Instant.parse(...)` использовать только в хелперах; в тестах, где конкретный
момент времени важен для бизнес-логики — объявлять отдельную константу с понятным именем.

---

### SOLID в тестах

Тесты — это тоже код. К ним применяются те же принципы, что и к production-коду.

#### SRP — один тест = одно поведение

Каждый тест проверяет ровно одну вещь. Несколько `assertThat` об одном объекте — допустимо. `verify()` об одном поведении — допустимо. Смешивать несвязанные утверждения — нет:

```java
// ❌ три ответственности в одном тесте
void create_task() {
  taskCommandService.create(...);
  assertThat(captor.getValue().status()).isEqualTo(PENDING);
  assertThat(captor.getValue().assignedTo()).isEqualTo(QA);
  verify(notificationService).notify(any());  // это уже другая история
}

// ✅ каждая ответственность — свой тест
void create_saves_task_with_pending_status()
void create_sets_assigned_role_from_parameter()
```

#### OCP — новый сценарий = новый тест, существующий не меняется

Добавление новой ветки в production-код → добавляем новый тест. Существующие тесты не трогаем. Если для нового сценария приходится менять старый тест — это сигнал, что старый тест проверял слишком много.

#### DRY — вспомогательные методы и базовые классы

Повторяющиеся объявления аннотаций для интеграционных тестов выносить в абстрактный базовый класс:

```java
// src/test/java/.../common/IntegrationTestBase.java
@SpringBootTest
@ActiveProfiles("tc")
@Transactional
@Tag("integration")
public abstract class IntegrationTestBase {}

// конкретный тест — только то, что уникально
class TaskJpaStoreTest extends IntegrationTestBase {
  @Autowired private TaskJpaStore taskJpaStore;

  @Test
  void save_and_findById_roundtrip() { ... }
}
```

Повторяющееся создание объектов — в приватные `taskModel()`, `existingTask()` методы (см. **Тестовые данные**).

**Исключение DRY в тестах**: если вынесение общего кода делает тест менее читаемым — оставить дублирование. Читаемость теста важнее DRY.

#### ISP — один тест-класс не тестирует всё подряд

Если тест-класс разрастается до 30+ методов — это сигнал, что production-класс нарушает SRP. Разбить production-класс на более мелкие, тест-классы разделятся сами.

#### DIP — тесты зависят от контрактов, не от реализаций

Мокировать интерфейсы (`TaskStore`), а не реализации (`TaskJpaStore`). Тест не должен знать о деталях инфраструктуры:

```java
@Mock private TaskStore taskStore;       // ✅ мокируем порт
@Mock private TaskJpaStore taskJpaStore; // ❌ мокируем реализацию — тест хрупкий
```

---

### TDD workflow

1. Тест → **красный** (реализации ещё нет)
2. Минимальная реализация → **зелёный**
3. Рефакторинг → **зелёный**

**Запуск**:

```bash
./gradlew test                              # unit-тесты (без БД)
./gradlew test -Dspring.profiles.active=tc  # интеграционные с Testcontainers
./gradlew spotlessCheck                     # форматирование (запускать до коммита)
```

**Запрещено в тестах**:

- **НЕ** писать реализацию до теста (TDD)
- **НЕ** оставлять непокрытый production-код — каждая ветка, каждый `catch`, каждый `case` должны быть протестированы
- **НЕ** проверять несколько несвязанных поведений в одном тесте (SRP)
- **НЕ** мокировать реализации (`*JpaStore`) вместо интерфейсов (`*Store`) (DIP)
- **НЕ** класть тест в другой подпакет, чем тестируемый класс
- **НЕ** использовать `ArgumentCaptor.forClass()` — только `ArgumentCaptor.captor()` (Mockito 5)
- **НЕ** поднимать `@SpringBootTest` для unit-теста — это медленно и хрупко
- **НЕ** использовать `@MockBean` в unit-тестах — только в интеграционных
- **НЕ** вызывать `Instant.now()` прямо в тестируемом коде — мокать `TimeProvider`
- **НЕ** дублировать логику теста в помощниках — помощник создаёт данные, а не проверяет
- **НЕ** дублировать аннотации `@SpringBootTest + @ActiveProfiles("tc") + @Transactional + @Tag` в каждом интеграционном тесте — выносить в `IntegrationTestBase`

---

## Миграции БД

Файлы в `src/main/resources/db/migration/V{n}__*.sql`.
`ddl-auto: validate` — Hibernate только валидирует.
При добавлении поля/таблицы **обязательно** создавать новый Flyway-скрипт.

---

## Профили Spring

| Профиль | Когда активен        | Что меняет                                        |
|---------|----------------------|---------------------------------------------------|
| _(нет)_ | локальная разработка | человекочитаемые логи, H2 не используется         |
| `tc`    | интеграционные тесты | Testcontainers поднимает PostgreSQL автоматически |
| `prod`  | production-окружение | structured JSON-логи (ECS формат)                 |

Активация: `./gradlew test -Dspring.profiles.active=tc` или `SPRING_PROFILES_ACTIVE=prod` в env.

---

## Observability

Метрики через **Micrometer** → Prometheus (`/actuator/prometheus`).

### Соглашения по метрикам

```java
// Timer — длительность операции
Timer.builder("devcrew.agent.duration")
    .tag("role", role.name())
    .register(meterRegistry)
    .record(duration, TimeUnit.MILLISECONDS);

// Counter — количество событий
Counter.builder("devcrew.task.total")
    .tag("role", role.name())
    .tag("status", "COMPLETED")
    .register(meterRegistry)
    .increment();
```

**Правила**:
- Префикс всех метрик: `devcrew.`
- Ключи тегов (`tag`) — строчными буквами snake_case: `"agent_role"`, `"status"` — не `"AgentRole"`; значения тегов — как есть (`role.name()` → `BACKEND_DEV`)
- `MeterRegistry` инжектируется только в `app/service/**`, не в контроллеры и не в domain
- Для unit-тестов с метриками: `new SimpleMeterRegistry()` — без `@SpringBootTest`

**Запрещено**: создавать метрики прямо в `@RestController` или `domain/`.

---

## Стиль кода

### Форматирование

Форматтер: **Spotless + Google Java Format** (настроен в `build.gradle`).

```bash
./gradlew spotlessApply   # применить форматирование
./gradlew spotlessCheck   # проверить без изменений (запускается в CI)
```

- Отступы Java: **2 пробела** (Google Java Format)
- Отступы YAML / Gradle: 2 пробела
- Длина строки: **100 символов**
- Строки: LF, UTF-8, финальный newline
- Неиспользуемые импорты удаляются автоматически (`removeUnusedImports`)

### var

Используй `var` везде, где тип очевиден из правой части выражения:

```java
var task = taskStore.findById(id);           // ✅ очевидно
var result = mapper.toModel(entity);         // ✅ очевидно
Map<UUID, List<TaskModel>> index = new ...   // ✅ сложный generic — тип явно
```

### Lombok

| Аннотация                  | Когда использовать                               |
|----------------------------|--------------------------------------------------|
| `@RequiredArgsConstructor` | DI через конструктор — всегда                    |
| `@Data`                    | Только на `*Properties`-классах                  |
| `@Builder`                 | Там где нужен builder (Entity, фабричные методы) |
| `@UtilityClass`            | Утилитные классы без состояния                   |
| `@Slf4j`                   | Логирование                                      |
| `@SneakyThrows`            | **Запрещено** — скрывает checked exceptions      |

Domain-модели — **record**, не Lombok-классы.

### MapStruct

Маппинг между слоями — только через MapStruct, `componentModel = "spring"`.
Ручная конвертация в сервисах — **запрещена**.

### Логирование

```java
log.debug("Запуск агента: taskId={}, role={}", taskId, role);  // детали выполнения
log.info("Агент завершил задачу: taskId={}", taskId);          // бизнес-события
log.warn("Повторная попытка: attempt={}", attempt);            // нештатные ситуации
log.error("Ошибка выполнения задачи: taskId={}", taskId, e);  // исключения с трейсом
```

- `log.debug` — детали внутри методов, входные параметры
- `log.info` — завершение значимых операций (агент выполнен, задача создана)
- `log.error` — всегда с объектом исключения вторым аргументом

### Прочее

- Комментарии в коде — **на русском**
- Без `I`-префикса у интерфейсов (`AgentStore`, не `IAgentStore`)

---

## Частые команды

```bash
./gradlew build                              # сборка + тесты
./gradlew test -Dspring.profiles.active=tc   # тесты с Testcontainers
./gradlew bootRun                            # локальный запуск
./gradlew spotlessApply                      # форматирование кода
./gradlew spotlessCheck                      # проверка форматирования (CI)

# Docker (все команды запускаются из корня проекта)
docker compose -f docker/docker-compose.yml up -d
docker compose -f docker/docker-compose.yml -f docker/docker-compose.prod.yml up -d
```

---

## Git Commit Conventions

### Формат

```
type(scope): краткое описание (≤ 72 символа)

Необязательное тело: объясняет «почему», а не «что».
Пустая строка между темой и телом обязательна.
```

### Типы

| Тип        | Когда использовать                                        |
|------------|-----------------------------------------------------------|
| `feat`     | Новая функциональность                                    |
| `fix`      | Исправление бага                                          |
| `refactor` | Изменение кода без смены поведения                        |
| `test`     | Добавление или исправление тестов                         |
| `chore`    | Обновление зависимостей, конфиги сборки                   |
| `docs`     | Документация (CLAUDE.md, README, javadoc)                 |
| `perf`     | Оптимизация производительности                            |

### Скоуп

Имя bounded context: `agent`, `task`, `auth`, `audit`, `organization`, `notification`, `common`.
При изменении нескольких — перечислить через запятую или опустить скоуп.

### Правила

- Тема: строчные буквы, повелительное наклонение на английском, без точки
  - ✅ `feat(agent): add timer metric for task execution`
  - ❌ `Added timer`, `Fix bug`, `WIP`
- Тело: отвечает на «зачем», а не «что» — это видно из diff
- **НЕ** коммитить «забытые файлы» — правим через `git commit --amend`
- **НЕ** смешивать рефакторинг + новую фичу в одном коммите

### Squash-политика

Перед PR вся цепочка `wip`/`fix test` коммитов схлопывается:
```bash
git rebase -i main
```

---

## Ветки и Pull Requests

### Именование веток

```
feat/scope-description      # новая функциональность
fix/scope-description       # исправление бага
refactor/scope-description  # рефакторинг
chore/scope-description     # зависимости, CI
```

### Гранулярность PR

- Один PR = одно логическое изменение
- Цель по размеру: ≤ 400 строк diff; крупнее — обосновать или разбить
- PR не смешивает `feat` + `refactor`

### Шаблон описания PR

```markdown
## Что изменилось
- bullet-point краткое описание

## Почему
Контекст: зачем это нужно, какую проблему решает.

## Как проверить
- [ ] `./gradlew test` — зелёный
- [ ] ArchUnit не нарушен
```

---

## Чего НЕ делать

### Архитектура

- **НЕ** добавлять JPA/Spring-аннотации в `domain/`
- **НЕ** вызывать `*Store` / `*Repository` напрямую из контроллеров
- **НЕ** нарушать направление зависимостей: `adapter → app → domain`
- **НЕ** хардкодить ключи API, токены, модели — только через `*Properties` + env vars

### Код

- **НЕ** хардкодить числа, строки-шаблоны, флаги
- **НЕ** использовать `Instant.now()` / `LocalDate.now()` напрямую — только `TimeProvider`
- **НЕ** добавлять `@Transactional` в контроллеры

### Тесты

Полный список запретов — в разделе **Тестирование** выше. Ключевые:

- **НЕ** писать реализацию до теста (TDD)
- **НЕ** поднимать `@SpringBootTest` там, где достаточно unit-теста с Mockito
- **НЕ** использовать `ArgumentCaptor.forClass()` — только `ArgumentCaptor.captor()`
- **НЕ** класть тест в другой подпакет, чем тестируемый класс
