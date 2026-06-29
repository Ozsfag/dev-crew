> **Когда читать**: трогаешь Gradle/Docker/БД/Flyway/Actuator/профили или
> добавляешь пункт roadmap (шаблон план-файла — внизу).

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
│   ├── architecture.md   ← C4, поток задачи, SOLID, именование
│   ├── coding.md         ← стиль кода, ConfigurationProperties, логирование
│   ├── testing.md        ← тестирование, TDD, правила покрытия
│   └── infra.md          ← инфраструктура, миграции, профили, observability
├── agents/               ← read-only субагенты-ревьюеры (канон) + README
├── skills/               ← вызываемые скиллы (review, скаффолды) + README
├── plans/                ← по одному файлу на пункт roadmap (шаблон — внизу)
└── PLAN.md               ← индекс roadmap (только ссылки)
```

Системные промпты агентов хранятся в `src/main/resources/prompts/<role>.md` и
загружаются `AgentDispatcher` из classpath, затем передаются в `claude` CLI как
содержимое временного `CLAUDE.md` (см. [architecture.md](architecture.md)).

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

## Docker

### Dockerfile (`docker/Dockerfile`)

Multi-stage образ. **Stage 1 (builder)** — `eclipse-temurin:21-jdk-alpine`,
кеширует зависимости, собирает `bootJar`. **Stage 2 (runtime)** — ставит
`claude` CLI (`npm install -g @anthropic-ai/claude-code`) + `git` (агенты клонируют
репозитории), создаёт непривилегированного `appuser` и sandbox-каталог `/projects`:

```dockerfile
RUN apk add --no-cache curl git nodejs npm && \
    npm install -g @anthropic-ai/claude-code --quiet && \
    addgroup -S appgroup && adduser -S appuser -G appgroup && \
    mkdir -p /projects && chown appuser:appgroup /projects
USER appuser   # уменьшает ущерб при container escape (агент исполняет код)
```

> Образу **обязательно** нужен `claude` CLI — это ядро исполнения агентов, а не
> опциональная зависимость.

### docker-compose

- `docker/docker-compose.yml` — локально; `docker-compose.prod.yml` — оверрайд для
  production. Секреты — через `${VAR:?Set ... in .env}` (fail-fast без значения),
  не дефолты в compose: `DEVCREW_AUTH_JWT_SECRET`, `DB_PASSWORD`, Stripe/Telegram токены.
- postgres — `healthcheck` + `depends_on: condition: service_healthy`; `restart: unless-stopped`.
- **Credentials Claude**: локально монтируются с хоста (`~/.claude.json`, `~/.claude/`).
  На сервере так **не** делать — нужен `claude login` в контейнере / named volume
  (токен сессии не должен лежать в bind-mount с хоста в проде).
- **Sandbox**: том проектов (`PROJECTS_ROOT:/projects`) — единственное, куда агент
  пишет; docker-сокет контейнеру не отдавать (= root на хосте).

---

## Actuator

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, prometheus
```

- `/actuator/health` — проверка здоровья (healthcheck контейнера).
- `/actuator/prometheus` — метрики Micrometer для Prometheus scrape.

Новый endpoint в `include` — осознанно: `env`/`configprops` не экспонировать
(утечка секретов).

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

---

## Управление задачами (ai/plans/)

Каждый пункт roadmap — отдельный файл `ai/plans/<id>-<slug>.md`. `ai/PLAN.md` —
только индекс со ссылками; содержательная часть — исключительно в `ai/plans/`.

**Правило**: добавляешь строку в таблицу `ai/PLAN.md` → **обязательно** создаёшь
файл плана со следующими разделами (команда `/task` это делает):

```markdown
## Контекст
Почему нужно, что случится если не сделать.

## Проблема
Точное описание пробела с указанием файлов.

## Техническое решение
Структура файлов, примеры кода, паттерны (по architecture.md / coding.md).

## Acceptance Criteria
- [ ] Конкретный проверяемый критерий

## Тест-план
Какие тесты (unit / controller / integration `-Dspring.profiles.active=tc`),
как запускать. TDD — тест до реализации.

## Refactoring
Обязательная мини-фаза (touched code only). Прогнать субагента
[`refactor-cleaner`](../agents/refactor-cleaner.md), применить findings или
обосновать. Долг вне touched code — запарковать строкой в `ai/PLAN.md`.

## Review (Skill /review)
Перед merge: `/review` — `./gradlew build` (ArchUnit + Spotless + тесты) + judgment-
субагенты по затронутому домену (таблица — [CLAUDE.md §«Code-review агенты»](../../CLAUDE.md)).
Минимум: build (всегда) + `java-reviewer` (если тронут `*.java`).

## Зависимости
Что должно быть выполнено до.
```

**Refactoring-фаза обязательна** в каждом плане с реализацией — часть DoD, не
отдельный пункт roadmap и не отдельный коммит. Нечего править в touched code —
отметить чек-лист пустым (валидный исход) и идти дальше.
