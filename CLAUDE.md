# CLAUDE.md — Dev Crew

## Что это за проект

Spring Boot 3.5 / Java 21 приложение. Оркестрирует команду ИИ-агентов для разработки программных проектов.
Архитектор (человек) согласовывает планы; агенты выполняют задачи автономно через LangChain4j + Claude API.

**Группа**: `org.blacksoil` | **Порт**: 8081 | **БД**: PostgreSQL + Flyway

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

| Пакет | Назначение |
|-------|-----------|
| `agent` | Определения агентов, роли, состояние, оркестрация |
| `task` | Задачи для агентов: создание, декомпозиция, статусы |
| `tool` | Инструменты агентов: git, gradle, файловая система |
| `memory` | Векторная память агентов, контекст проекта |
| `notification` | Уведомления архитектору (Telegram) |
| `common` | Shared: exceptions, TimeProvider, web error handling |

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

## Соглашения по именованию

| Суффикс | Что это |
|---------|---------|
| `*Model` | Immutable record в domain |
| `*Entity` | JPA-сущность в adapter/out |
| `*Dto` | DTO в app-слое |
| `*Request` / `*Response` | DTO в web-слое |
| `*Store` | Port-интерфейс (domain) |
| `*JpaStore` | Реализация Store (adapter/out) |
| `*Repository` | Spring Data interface |
| `*Orchestrator` | Координирует несколько сервисов |
| `*Mapper` | MapStruct-интерфейс (componentModel = "spring") |
| `*Factory` | Конструирует domain-объекты |
| `*Hook` | Extension point (OCP-паттерн) |
| `*Properties` | `@ConfigurationProperties` класс |
| `*Config` | `@Configuration` + `@EnableConfigurationProperties` |
| `*Policy` | Стратегия/правило |
| `*Agent` | LangChain4j AiService-интерфейс агента |

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
public class AgentConfig {}
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
    public String runTests(String projectPath) { ... }

    @Tool("Build the Gradle project")
    public String buildProject(String projectPath) { ... }
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

## Тестирование

| Тип | Подход |
|-----|--------|
| Unit | `@ExtendWith(MockitoExtension.class)` + AssertJ, без `@SpringBootTest` |
| Controller (standalone) | `MockMvcBuilders.standaloneSetup()` |
| Integration | `@SpringBootTest` + Testcontainers (профиль `tc`) |

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

## Стиль кода

- Отступы: 4 пробела (Java), 2 пробела (YAML / Gradle)
- Строки: LF, UTF-8, финальный newline
- Lombok: `@RequiredArgsConstructor` для DI, `@Builder` там где нужен builder, `@Data` для `*Properties`, для утилитных классов `@UtilityClass`. Используй Lombok на полную.
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
