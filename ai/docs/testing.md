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

Именование тестов контроллеров: `МЕТОД_path_ожидаемоеПоведение`:
```java
void POST_tasks_returns_201_with_task_id()
void GET_tasks_id_returns_404_when_missing()
void POST_tasks_returns_400_when_title_missing()
```

---

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
    when(taskStore.save(any())).thenAnswer(inv -> inv.getArgument(0));

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
          agentDispatcher,
          taskQueryService,
          taskCommandService,
          List.of(postAgentHook),       // ← List собираем вручную
          new SimpleMeterRegistry(),
          rateLimitPolicy,
          timeProvider);
}
```

**`@ConfigurationProperties`-классы** тестировать через `new FooProperties()`:

```java
var properties = new RateLimitProperties();  // default retry-delay = 60s
var policy = new RateLimitPolicy(properties);
```

### Тесты контроллеров (standalone MockMvc)

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

### Тесты с метриками (Micrometer)

Вместо `@SpringBootTest` — `new SimpleMeterRegistry()`:

```java
private SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

@Test
void execute_increments_COMPLETED_counter_on_success() {
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

Использовать фиксированный `Instant.parse(...)` — не `Instant.now()`. `Instant.parse(...)` только в хелперах; в тестах, где момент времени важен для бизнес-логики — объявлять отдельную константу.

---

### SOLID в тестах

#### SRP — один тест = одно поведение

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

Добавление новой ветки в production-код → добавляем новый тест. Если для нового сценария приходится менять старый тест — это сигнал, что старый тест проверял слишком много.

#### DRY — вспомогательные методы и базовые классы

```java
// Повторяющиеся аннотации → IntegrationTestBase
@SpringBootTest
@ActiveProfiles("tc")
@Transactional
@Tag("integration")
public abstract class IntegrationTestBase {}
```

#### DIP — тесты зависят от контрактов, не от реализаций

```java
@Mock private TaskStore taskStore;       // ✅ мокируем порт
@Mock private TaskJpaStore taskJpaStore; // ❌ мокируем реализацию — тест хрупкий
```

---

### TDD workflow

1. Тест → **красный** (реализации ещё нет)
2. Минимальная реализация → **зелёный**
3. Рефакторинг → **зелёный**

```bash
./gradlew test                              # unit-тесты (без БД)
./gradlew test -Dspring.profiles.active=tc  # интеграционные с Testcontainers
./gradlew spotlessCheck                     # форматирование (запускать до коммита)
```

---

### Запрещено в тестах

- **НЕ** писать реализацию до теста (TDD)
- **НЕ** оставлять непокрытый production-код — каждая ветка, каждый `catch`, каждый `case`
- **НЕ** проверять несколько несвязанных поведений в одном тесте (SRP)
- **НЕ** мокировать реализации (`*JpaStore`) вместо интерфейсов (`*Store`) (DIP)
- **НЕ** класть тест в другой подпакет, чем тестируемый класс
- **НЕ** использовать `ArgumentCaptor.forClass()` — только `ArgumentCaptor.captor()` (Mockito 5)
- **НЕ** поднимать `@SpringBootTest` для unit-теста — это медленно и хрупко
- **НЕ** использовать `@MockBean` в unit-тестах — только в интеграционных
- **НЕ** вызывать `Instant.now()` в тестируемом коде — мокать `TimeProvider`
- **НЕ** дублировать аннотации `@SpringBootTest + @ActiveProfiles("tc") + @Transactional + @Tag` — выносить в `IntegrationTestBase`
