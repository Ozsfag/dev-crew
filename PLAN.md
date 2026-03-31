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
- [x] `agent` — AgentModel, AgentStore, AgentQueryService, BackendDevAgent (LangChain4j), AgentExecutionService, AgentOrchestratorImpl
- [x] `task` — TaskModel, TaskStore, TaskQueryService, TaskCommandService
- [x] `notification` — NotificationPort, TelegramNotificationAdapter (PostAgentHook)
- [x] `common` — TimeProvider, DomainException, GlobalExceptionHandler

### REST API
- [x] `GET /api/agents`, `GET /api/agents/{id}`
- [x] `POST /api/tasks`, `POST /api/tasks/{id}/run`, `GET /api/tasks/{id}`

### Тесты
- [x] 49 unit-тестов (все зелёные), ArchUnit, @Tag("integration") для JPA-тестов

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

#### П2.1 — JWT аутентификация
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

#### П2.2 — Audit log
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

#### П3.1 — GitTools
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

#### П3.2 — QaAgent
System prompt: `prompts/qa.txt`
Tools: FileTools, GradleTools (runTests, checkCoverage)
Задача: получает путь к модулю → пишет тесты → прогоняет → репортит покрытие.

---

#### П3.3 — CodeReviewAgent
System prompt: `prompts/code-review.txt`
Tools: FileTools, GitTools (gitDiff)
Задача: получает PR diff → проверяет по чеклисту (архитектура, безопасность, N+1) → возвращает review.

---

#### П3.4 — DevOpsAgent
System prompt: `prompts/devops.txt`
Tools: FileTools, CommandRunner (docker build/push), GitTools
Задача: обновляет docker-compose, Dockerfile, CI/CD конфиги.

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

## Текущий приоритет выполнения

```
✅ П1.1  Async выполнение агентов
✅ П1.2  SandboxPolicy для FileTools
П2.1  JWT аутентификация               ← СЕЙЧАС
П2.1  JWT аутентификация
П2.2  Audit log
П3.1  GitTools
П3.2  QaAgent
П3.3  CodeReviewAgent
П3.4  DevOpsAgent
П4    Мультитенантность
П5    Observability (Prometheus + Grafana)
П6    Монетизация (Stripe)
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
