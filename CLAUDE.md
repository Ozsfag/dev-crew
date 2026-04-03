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
│   ├── devops.md
│   └── ...
└── templates/            ← шаблоны задач, few-shot примеры
```

Промпты загружаются через `AgentProperties` из `application.yml` — не хардкодятся в Java.

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
| `common`       | Shared: exceptions, TimeProvider, web error handling  |

---

## Структура подпакетов: agent (эталон)

```
agent/app/
├── config/
│   ├── AgentConfig.java             ← @EnableConfigurationProperties
│   └── AgentProperties.java         ← devcrew.agent.*
└── service/
    ├── execution/   ← AgentExecutionService
    ├── query/       ← AgentQueryService
    └── AgentOrchestratorImpl.java   ← оркестратор на верхнем уровне
```

**Правило**: сервисы группируются по ответственности в подпакеты. Плоский `app/service/` с 5+ классами — запрещён.

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
└── domain/               ← OrganizationModel.java, OrganizationStore.java (port-интерфейс)
```

| Тип                                        | Подпакет                              |
|--------------------------------------------|---------------------------------------|
| `@RestController`                          | `adapter/in/web/controller/`          |
| `@Mapper / *WebMapper` (MapStruct)         | `adapter/in/web/mapper/`              |
| `*Request` / `*Response`                   | `adapter/in/web/dto/`                 |
| `@Entity`                                  | `adapter/out/persistence/entity/`     |
| `@Mapper / *PersistenceMapper` (MapStruct) | `adapter/out/persistence/mapper/`     |
| `@Repository / *Repository` (Spring Data)  | `adapter/out/persistence/repository/` |
| `*JpaStore` (реализация порта)             | `adapter/out/persistence/store/`      |
| `*Store`, `*Model`, `*Orchestrator`, `*Agent`, `*Hook`, `*Exception` | `domain/` (плоский пакет) |

**Запрещено**: класть в одну папку разнородные типы (например, `Entity` + `Repository` + `JpaStore` в одном плоском
`persistence/`).

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
| `*Hook`                  | Extension point (OCP-паттерн)                       |
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

- **SRP**: каждый context (`agent`, `task`, `auth`, `audit`, `notification`) отвечает за одну область. Изменение в
  `audit` не затрагивает `agent`.
- **OCP**: новые поведения добавляются через порты (`PostAgentHook`), а не через изменение существующих сервисов.
- **Stable Abstractions**: `domain/` — только интерфейсы и records (стабильные, почти не меняются). `adapter/out/` —
  конкретные реализации (нестабильные, меняются чаще).

### Уровень 3: Зависимости между компонентами

```
notification → agent → domain
audit        → agent → domain
          (стрелка = "зависит от")
```

- **Acyclic Dependencies Principle (ADP)**: нет циклов между контекстами. `notification` знает об `agent` через
  `PostAgentHook`; `agent` не знает о `notification`. Циклы = запрещены.
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

| Тип                     | Подход                                                                 |
|-------------------------|------------------------------------------------------------------------|
| Unit                    | `@ExtendWith(MockitoExtension.class)` + AssertJ, без `@SpringBootTest` |
| Controller (standalone) | `MockMvcBuilders.standaloneSetup()`                                    |
| Integration             | `@SpringBootTest` + Testcontainers (профиль `tc`)                      |

**Правила тестов**:

- Тест живёт в том же подпакете, что и тестируемый класс
- `ArgumentCaptor` в Mockito 5: использовать `ArgumentCaptor.captor()`, не `ArgumentCaptor.forClass()`
- Для unit-тестов `@ConfigurationProperties`-классов: создавать через `new FooProperties()`

**TDD workflow**:

1. Тест → красный
2. Минимальная реализация → зелёный
3. Рефакторинг → зелёный

**Запуск**:

```bash
./gradlew test                              # unit-тесты (без БД)
./gradlew test -Dspring.profiles.active=tc  # интеграционные с Testcontainers
```

---

## Миграции БД

Файлы в `src/main/resources/db/migration/V{n}__*.sql`.
`ddl-auto: validate` — Hibernate только валидирует.
При добавлении поля/таблицы **обязательно** создавать новый Flyway-скрипт.

---

## Профили Spring

| Профиль | Когда активен | Что меняет |
|---------|---------------|------------|
| _(нет)_ | локальная разработка | человекочитаемые логи, H2 не используется |
| `tc`    | интеграционные тесты | Testcontainers поднимает PostgreSQL автоматически |
| `prod`  | production-окружение | structured JSON-логи (ECS формат) |

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
- Теги (`tag`) — строчными буквами snake_case: `agent_role`, не `AgentRole`
- `MeterRegistry` инжектируется только в `app/service/**`, не в контроллеры и не в domain
- Для unit-тестов с метриками: `new SimpleMeterRegistry()` — без `@SpringBootTest`

**Запрещено**: создавать метрики прямо в `@RestController` или `domain/`.

---

## Стиль кода
- используй Google java format style для форматирования
- Отступы: 4 пробела (Java), 2 пробела (YAML / Gradle)
- Строки: LF, UTF-8, финальный newline
- Lombok: `@RequiredArgsConstructor` для DI, `@Builder` там где нужен builder, `@Data` для `*Properties`, для утилитных
  классов `@UtilityClass`. Используй Lombok на полную.
- MapStruct: для маппинга между слоями, `componentModel = "spring"`
- Комментарии в коде — **на русском**
- Без `I`-префикса у интерфейсов
- Domain-модели — **record**

---

## Частые команды

```bash
./gradlew build                              # сборка + тесты
./gradlew test -Dspring.profiles.active=tc   # тесты с Testcontainers
./gradlew bootRun                            # локальный запуск
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

- **НЕ** писать реализацию до теста (TDD)
- **НЕ** класть тест в другой подпакет, чем тестируемый класс
- **НЕ** использовать `ArgumentCaptor.forClass()` — только `ArgumentCaptor.captor()`
