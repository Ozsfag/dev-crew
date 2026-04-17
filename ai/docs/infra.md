## Структура корневого каталога

В корне проекта обязательно присутствуют:

```
dev-crew/
├── ai/        ← контекст работы с Claude: документация, PLAN.md
├── docker/    ← Dockerfile, docker-compose файлы, конфиги окружений
└── src/
```

**`ai/`** — документация и планирование:

```
ai/
├── docs/
│   ├── architecture.md   ← архитектура, SOLID, именование
│   ├── coding.md         ← стиль кода, LangChain4j, ConfigurationProperties
│   ├── testing.md        ← тестирование, TDD, правила покрытия
│   └── infra.md          ← инфраструктура, миграции, профили, observability
└── PLAN.md               ← roadmap разработки
```

Системные промпты агентов хранятся в `src/main/resources/prompts/` и загружаются через
LangChain4j `@SystemMessage(fromResource = "prompts/*.md")`.

**`docker/`** — инфраструктурные файлы:

```
docker/
├── Dockerfile
├── docker-compose.yml
└── docker-compose.prod.yml
```

Docker-файлы **не держать в корне проекта**.

---

## Миграции БД

Файлы в `src/main/resources/db/migration/V{n}__*.sql`.
`ddl-auto: validate` — Hibernate только валидирует.
При добавлении поля/таблицы **обязательно** создавать новый Flyway-скрипт.

---

## Профили Spring

| Профиль | Когда активен        | Что меняет                                        |
|---------|----------------------|---------------------------------------------------|
| _(нет)_ | локальная разработка | человекочитаемые логи                             |
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
- Ключи тегов — строчными буквами snake_case: `"agent_role"`, `"status"`
- Значения тегов — как есть (`role.name()` → `BACKEND_DEV`)
- `MeterRegistry` инжектируется только в `app/service/**`, не в контроллеры и не в domain

**Запрещено**: создавать метрики прямо в `@RestController` или `domain/`.
