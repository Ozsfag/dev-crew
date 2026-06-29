> **Когда читать**: проектируешь bounded context / порт / адаптер, межконтекстный
> вызов, новый поток выполнения; нужны C4-обзор, поток задачи, гексагональные
> слои, naming, SOLID, границы.

# Architecture

Обзор архитектуры Dev Crew: C4-обзор, поток выполнения задачи, гексагональные
слои, bounded contexts, SOLID. Стиль кода — [coding.md](coding.md);
тесты — [testing.md](testing.md); инфраструктура — [infra.md](infra.md).

## C4 Level 1 — Context

```
   ┌──────────────────┐  REST / Telegram   ┌───────────────────────────────┐
   │   Архитектор     │ ─────────────────► │        Dev Crew (:8081)       │
   │    (человек)     │ ◄───────────────── │  оркестрирует команду агентов │
   └──────────────────┘   уведомления      └───────┬───────────────┬───────┘
                                                   │               │
                          claude CLI (subprocess)  │               │ JPA
                          в sandbox /projects       ▼               ▼
                                          ┌──────────────┐  ┌──────────────┐
                                          │  Claude Code │  │ PostgreSQL   │
                                          │     CLI      │  │ (задачи,     │
                                          │ (пишет код,  │  │  агенты,     │
                                          │  гоняет тесты)│  │  org, биллинг)│
                                          └──────────────┘  └──────────────┘
```

## C4 Level 2 — Containers

```
                 ┌──────────────────────────────────────────┐
 external HTTP ─►│         Dev Crew Spring Boot :8081        │
   + JWT         │  adapter/in (REST) → app (@Service) →     │
                 │  domain (порты) → adapter/out             │
                 └──┬─────────────┬──────────────┬───────────┘
                    │ subprocess  │ HTTP         │ JDBC
                    ▼             ▼              ▼
            ┌──────────────┐ ┌──────────┐ ┌──────────────┐
            │  claude CLI  │ │ Telegram │ │ PostgreSQL   │
            │ (per-task    │ │  / Stripe│ │  + Flyway    │
            │  temp dir)   │ │   API    │ │              │
            └──────────────┘ └──────────┘ └──────────────┘
```

> Ядро исполнения — **не LangChain4j**, а `claude` CLI, запускаемый подпроцессом
> (`ClaudeCodeRunnerImpl`). Роль агента = системный промпт `prompts/<role>.md`,
> записываемый во временный `CLAUDE.md`.

## Поток: выполнение задачи агентом

```
1. POST /api/tasks               → AgentOrchestrator.submit → задача PENDING в БД
2. POST /api/tasks/{id}/run      → AgentOrchestrator.run (проверка org-тенанта)
3. AgentExecutionService.execute (@Async, virtual thread): статус → IN_PROGRESS
4. AgentDispatcher: грузит prompts/<role>.md → ClaudeCodeRunner
5. ClaudeCodeRunnerImpl: temp-dir + CLAUDE.md + .claude/settings.json →
   ProcessBuilder `claude --print <task> --output-format json` в sandbox /projects
6. результат распарсен → статус COMPLETED / FAILED / RATE_LIMITED; метрики записаны
7. PostAgentHook(s) → TelegramNotificationAdapter уведомляет архитектора
8. RateLimitRetryScheduler (@Scheduled): повторяет RATE_LIMITED, когда retryAt наступил
```

## Технологический стек

| Слой          | Технология                                                        |
|---------------|-------------------------------------------------------------------|
| JVM           | Java 21 (Eclipse Temurin)                                         |
| Framework     | Spring Boot 3.5                                                   |
| Build         | Gradle + Spotless (Google Java Format, 2 пробела, 100 символов)   |
| ORM / DB      | Hibernate + Spring Data JPA · PostgreSQL + Flyway (`ddl-auto: validate`) |
| Mapping       | MapStruct 1.6.3 (`componentModel = "spring"`)                     |
| Auth          | Spring Security + JWT (jjwt 0.12.6)                               |
| Rate limiting | Bucket4j 8.10 + Caffeine                                          |
| Биллинг       | Stripe Java 28.3 (webhook)                                        |
| Исполнение LLM| Claude Code CLI (`@anthropic-ai/claude-code`) через subprocess    |
| Уведомления   | Telegram                                                          |
| Метрики       | Micrometer → Prometheus (`/actuator/prometheus`)                  |
| Тесты         | JUnit 5, Mockito 5, AssertJ, Testcontainers, MockMvc (standalone) |
| Арх-инварианты| ArchUnit (закрепляет `adapter → app → domain`)                   |

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
| `common`       | Shared: exceptions, TimeProvider, PageResult, web error handling |

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
│       └── AgentOrchestratorImpl.java
├── bootstrap/
│   ├── LangChain4jAgentConfig.java  ← создаёт бины LangChain4j-агентов
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
`adapter/out/` (нарушало бы правило `app → domain`, если разместить в `app/config/`).

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

### Структура подпакетов: notification (упрощённый контекст)

```
notification/
├── adapter/out/telegram/
│   ├── TelegramApiClient.java
│   ├── TelegramApiClientImpl.java
│   └── TelegramNotificationAdapter.java ← реализует NotificationPort + PostAgentHook
├── app/config/
│   ├── TelegramConfig.java
│   └── TelegramProperties.java
├── bootstrap/
│   └── TelegramClientConfig.java
└── domain/
    └── NotificationPort.java
```

---

## Структура пакетов: расположение файлов

```
organization/
├── adapter/
│   ├── in/web/
│   │   ├── controller/   ← OrganizationController.java  (@RestController)
│   │   ├── mapper/       ← OrganizationWebMapper.java   (*WebMapper)
│   │   └── dto/          ← CreateOrganizationRequest.java
│   └── out/persistence/
│       ├── entity/       ← OrganizationEntity.java      (@Entity)
│       ├── mapper/       ← OrganizationPersistenceMapper.java
│       ├── repository/   ← OrganizationRepository.java  (JpaRepository)
│       └── store/        ← OrganizationJpaStore.java    (*JpaStore)
├── app/
│   ├── config/
│   └── service/
│       ├── command/      ← OrganizationCommandService.java
│       └── query/        ← OrganizationQueryService.java
└── domain/
    ├── model/            ← OrganizationModel.java
    └── store/            ← OrganizationStore.java
```

| Тип                          | Подпакет                              |
|------------------------------|---------------------------------------|
| `@RestController`            | `adapter/in/web/controller/`          |
| `*WebMapper` (MapStruct)     | `adapter/in/web/mapper/`              |
| `*Request` / `*Response`     | `adapter/in/web/dto/`                 |
| `@Entity`                    | `adapter/out/persistence/entity/`     |
| `*PersistenceMapper`         | `adapter/out/persistence/mapper/`     |
| `*Repository` (Spring Data)  | `adapter/out/persistence/repository/` |
| `*JpaStore`                  | `adapter/out/persistence/store/`      |
| `*Model` (records)           | `domain/model/`                       |
| `*Store` (port-интерфейс)    | `domain/store/`                       |
| `*Agent` (LangChain4j)       | `domain/agent/`                       |
| `*Hook`                      | `domain/hook/`                        |
| `*Check`                     | `domain/check/`                       |
| Orchestrator, Exception, enum| `domain/` (корень)                    |

**Запрещено**: класть в одну папку разнородные типы.

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
| `PageResult<T>`          | Обёртка пагинации в `common/` (вместо Spring `Page`) |

Без `I`-префикса у интерфейсов (`AgentStore`, не `IAgentStore`).

---

## Принципы SOLID

### Уровень 1: Классы

| Принцип | Правило                                       | Пример в проекте                                                                        |
|---------|-----------------------------------------------|-----------------------------------------------------------------------------------------|
| **SRP** | Один мотив для изменения                      | `AgentExecutionService` выполняет задачи; `AgentQueryService` читает статус             |
| **OCP** | Открыт для расширения, закрыт для модификации | `PostAgentHook` — новые реакции без правки ядра                                         |
| **LSP** | Подтип подставляем везде                      | `AgentJpaStore` реализует `AgentStore` — любая реализация взаимозаменяема               |
| **ISP** | Узкие интерфейсы                              | Отдельные `*Store`, `*QueryPort` с минимальным контрактом                               |
| **DIP** | Зависимость от абстракций                     | Сервисы получают `AgentStore`, а не `AgentJpaStore`                                     |

### Уровень 2: Компоненты (bounded contexts)

- **SRP**: каждый context отвечает за одну область. Изменение в `audit` не затрагивает `agent`.
- **OCP**: новые поведения добавляются через порты (`PostAgentHook`), а не через изменение сервисов.
- **Stable Abstractions**: `domain/` — только интерфейсы и records (стабильные). `adapter/out/` — конкретные реализации (нестабильные).

### Уровень 3: Зависимости между компонентами

```
notification → agent → domain
audit        → agent → domain
billing      → agent → domain   (PostAgentHook, PreRunCheck)
billing      → task             (получает task по taskId)
billing      → organization     (получает OrgPlan для проверки лимита)
```

- **ADP**: нет циклов. `notification` знает об `agent` через `PostAgentHook`; `agent` не знает о `notification`.
- **SDP**: нестабильные модули зависят от стабильных (domain-интерфейсов).
- **SAP**: `domain/` — самые стабильные и абстрактные. Никогда не зависит от Spring или JPA.

**Запрещено**:
- Циклические зависимости между bounded contexts
- Один класс с несколькими несвязанными ответственностями
- «God-интерфейс» — дробить на узкие порты
- Зависимость от `*JpaStore` — только от `*Store`
