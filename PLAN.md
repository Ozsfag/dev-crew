# Dev Crew — Plan разработки

## Контекст проекта

Spring Boot 3.5 / Java 21 приложение для оркестрации команды ИИ-агентов.
Архитектор (человек) согласовывает планы; агенты выполняют задачи автономно через LangChain4j + Claude API.

**Группа**: `org.blacksoil` | **Порт**: 8081 | **Пакет**: `org.blacksoil.devcrew`
**Правила архитектуры и стиля**: см. `CLAUDE.md`

---

## Текущее состояние (что уже сделано)

### Инфраструктура
- [x] `build.gradle` — Spring Boot 3.5, LangChain4j 0.36.2, ArchUnit, Testcontainers
- [x] `application.yml` (порт 8081) + `application-tc.yml` (Testcontainers профиль)
- [x] `V1__init.sql` — таблицы `agents`, `tasks`
- [x] `.gitignore`, `settings.gradle`, `gradle/wrapper/gradle-wrapper.properties`

### common
- [x] `TimeProvider` — абстракция над `Instant.now()` / `LocalDate.now()`
- [x] `DomainException`, `NotFoundException`

### agent (bounded context)
- [x] `AgentRole` enum — ORCHESTRATOR, BACKEND_DEV, QA, DEVOPS, PM, ANALYST, CODE_REVIEWER
- [x] `AgentStatus` enum — IDLE, RUNNING, WAITING_FOR_APPROVAL, COMPLETED, FAILED
- [x] `AgentModel` record — id, role, status, systemPrompt, createdAt, updatedAt
- [x] `AgentStore` port-интерфейс — save, findById, findByRole, findAll
- [x] `PostAgentHook` — OCP extension point (вызывается после завершения агента)
- [x] `AgentProperties` — `devcrew.agent.*` (model, maxTokens, maxIterations)
- [x] `AgentConfig` — `@EnableConfigurationProperties`
- [x] `AgentQueryService` — getById, getByRole, getAll (+ unit-тест)

### task (bounded context)
- [x] `TaskStatus` enum — PENDING, IN_PROGRESS, WAITING_APPROVAL, APPROVED, REJECTED, COMPLETED, FAILED
- [x] `TaskModel` record — id, parentTaskId, title, description, assignedTo, status, result, timestamps
- [x] `TaskStore` port-интерфейс — save, findById, findByStatus, findByParentTaskId

### Тесты
- [x] `HexagonalArchitectureTest` — 5 ArchUnit правил (domain без Spring, нет зависимостей вверх)
- [x] `AgentQueryServiceTest` — 4 unit-теста (TDD-пример)

---

## Что нужно реализовать (по порядку)

### ~~Шаг 1 — adapter/out для agent (JPA-слой)~~ ✅ DONE
**Файлы для создания:**

```
agent/adapter/out/persistence/
├── AgentEntity.java               ← @Entity, table = "agents"
├── AgentRepository.java           ← Spring Data JPA
├── AgentPersistenceMapper.java    ← MapStruct: AgentModel ↔ AgentEntity
└── AgentJpaStore.java             ← implements AgentStore
```

**Детали AgentEntity:**
```java
@Entity @Table(name = "agents")
// поля: UUID id, AgentRole role, AgentStatus status,
//       String systemPrompt, Instant createdAt, Instant updatedAt
// role и status хранить как VARCHAR через @Enumerated(EnumType.STRING)
```

**Детали AgentJpaStore:**
```java
@Repository @RequiredArgsConstructor
// делегирует в AgentRepository + маппит через AgentPersistenceMapper
// findByRole: AgentRepository.findByRole(role) → map
```

**Тест:** `AgentJpaStoreTest` — `@SpringBootTest` + Testcontainers (профиль `tc`)

---

### ~~Шаг 2 — adapter/out для task (JPA-слой)~~ ✅ DONE
Аналогично шагу 1, но для `task`:

```
task/adapter/out/persistence/
├── TaskEntity.java
├── TaskRepository.java
├── TaskPersistenceMapper.java
└── TaskJpaStore.java              ← implements TaskStore
```

**Детали TaskEntity:**
```java
// поля: UUID id, UUID parentTaskId, String title, String description,
//       AgentRole assignedTo, TaskStatus status, String result,
//       Instant createdAt, Instant updatedAt
// parentTaskId nullable (@ManyToOne или просто UUID)
```

---

### ~~Шаг 3 — task/app (сервисный слой)~~ ✅ DONE
```
task/app/
├── config/
│   ├── TaskProperties.java        ← devcrew.task.* (пока пусто, но структура нужна)
│   └── TaskConfig.java
└── service/
    ├── query/TaskQueryService.java    ← getById, getByStatus, getSubtasks
    └── command/TaskCommandService.java ← create, updateStatus, complete, fail
```

**Детали TaskCommandService:**
```java
// create(String title, String description, AgentRole assignedTo, UUID parentTaskId)
//   → создаёт TaskModel со статусом PENDING, сохраняет через TaskStore
// updateStatus(UUID taskId, TaskStatus newStatus)
// complete(UUID taskId, String result)
// fail(UUID taskId, String reason)
```

**Тесты:** unit-тесты для `TaskQueryService` и `TaskCommandService` с Mockito

---

### ~~Шаг 4 — LangChain4j: первый агент BackendDevAgent~~ ✅ DONE
```
agent/adapter/out/llm/
├── tools/
│   ├── FileTools.java             ← @Tool: readFile, writeFile
│   └── GradleTools.java           ← @Tool: runTests, buildProject
└── BackendDevAgentAdapter.java    ← implements BackendDevAgent (domain port)

agent/domain/
└── BackendDevAgent.java           ← interface (AiService контракт)

agent/bootstrap/
└── LangChain4jAgentConfig.java    ← @Bean: AiServices.builder(BackendDevAgent.class)...
```

**Детали BackendDevAgent (domain port):**
```java
public interface BackendDevAgent {
    String execute(@UserMessage String task);
}
```

**Детали LangChain4jAgentConfig:**
```java
@Bean
public BackendDevAgent backendDevAgent(
    ChatLanguageModel model,
    AgentProperties properties,
    FileTools fileTools,
    GradleTools gradleTools
) {
    return AiServices.builder(BackendDevAgent.class)
        .chatLanguageModel(model)
        .tools(fileTools, gradleTools)
        .build();
}
```

**System prompt** для BackendDevAgent (хранить в `src/main/resources/prompts/backend-dev.txt`):
```
You are a Senior Java/Spring Boot developer.
Architecture: Hexagonal (Ports & Adapters). Rules defined in CLAUDE.md.
Always follow TDD: write failing test first, then minimal implementation.
Never use Instant.now() — always use TimeProvider.
Never hardcode values — use @ConfigurationProperties.
```

---

### ~~Шаг 5 — agent/app: AgentExecutionService + OrchestratorImpl~~ ✅ DONE
```
agent/app/service/
├── execution/AgentExecutionService.java   ← запускает агента на задачу
└── AgentOrchestratorImpl.java             ← координирует весь workflow
```

**Детали AgentExecutionService:**
```java
// execute(UUID taskId, AgentRole role, String prompt)
//   → находит агента по role
//   → делегирует в соответствующий *Agent (через port)
//   → вызывает List<PostAgentHook>.onAgentCompleted(...)
//   → обновляет статус задачи через TaskCommandService
```

---

### ~~Шаг 6 — adapter/in: REST API~~ ✅ DONE
```
agent/adapter/in/web/
├── AgentController.java           ← GET /api/agents, GET /api/agents/{id}
└── dto/AgentResponse.java

task/adapter/in/web/
├── TaskController.java            ← POST /api/tasks, GET /api/tasks/{id}
└── dto/
    ├── CreateTaskRequest.java
    └── TaskResponse.java
```

---

### ~~Шаг 7 — notification (bounded context)~~ ✅ DONE
```
notification/
├── domain/NotificationPort.java   ← sendApprovalRequest(String message): void
└── adapter/out/telegram/
    ├── TelegramProperties.java    ← devcrew.notification.telegram.*
    ├── TelegramConfig.java
    └── TelegramNotificationAdapter.java  ← implements NotificationPort
```

Реализует `PostAgentHook` — при `WAITING_APPROVAL` шлёт сообщение архитектору в Telegram.

---

## Порядок приоритетов

```
✅ Шаг 1 (AgentJpaStore)
✅ Шаг 2 (TaskJpaStore)
✅ Шаг 3 (task/app services)
✅ Шаг 4 (BackendDevAgent + Tools)
✅ Шаг 5 (Orchestrator + ExecutionService)
✅ Шаг 6 (REST API)
✅ Шаг 7 (Telegram notifications)
```

## Что осталось (следующие итерации)

- [ ] Установить Docker → запустить интеграционные тесты (@Tag integration)
- [ ] Добавить агентов: QaAgent, DevOpsAgent, PmAgent, AnalystAgent (по аналогии с BackendDevAgent)
- [ ] GitTools (@Tool: gitCommit, gitPush, createBranch)
- [ ] Добавить `ANTHROPIC_API_KEY` и `TELEGRAM_BOT_TOKEN` в .env, запустить приложение
- [ ] Эндпоинт `GET /api/tasks?status=PENDING` для просмотра очереди

---

## Напоминания по стилю (из CLAUDE.md)

- `ArgumentCaptor` → `ArgumentCaptor.captor()` (не `forClass()`) — Mockito 5
- Комментарии в коде — на русском
- `@Transactional` — только в `app/service/**` и `adapter/out/**`, никогда в контроллерах
- MapStruct mapper: `@Mapper(componentModel = "spring")`
- Enum в БД: `@Enumerated(EnumType.STRING)`
- Новое поле/таблица = новый Flyway-скрипт `V{n}__*.sql`
