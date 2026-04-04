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
│   └── infra.md          ← инфраструктура, мигации, профили, observability
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

---

## Git Commit Conventions

### Формат

```
type(scope): краткое описание (≤ 72 символа)

Необязательное тело: объясняет «почему», а не «что».
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

### Правила

- Тема: строчные буквы, повелительное наклонение на английском, без точки
  - ✅ `feat(agent): add timer metric for task execution`
  - ❌ `Added timer`, `Fix bug`, `WIP`
- **НЕ** смешивать рефакторинг + новую фичу в одном коммите

---

## Ветки и Pull Requests

### Именование веток

```
feat/scope-description
fix/scope-description
refactor/scope-description
chore/scope-description
```

### Шаблон описания PR

```markdown
## Что изменилось
- bullet-point

## Почему
Контекст: зачем это нужно, какую проблему решает.

## Как проверить
- [ ] `./gradlew test` — зелёный
- [ ] ArchUnit не нарушен
```

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

---

## Управление задачами (ai/plans/)

Каждая задача roadmap — отдельный файл в `ai/plans/`.

**Правило**: при добавлении нового пункта в `ai/PLAN.md` (таблицу roadmap) **ОБЯЗАТЕЛЬНО**
создавать файл `ai/plans/<id>-<slug>.md` со следующими разделами:

```markdown
## Контекст
Почему нужно, что случится если не сделать.

## Проблема
Точное описание пробела с указанием файлов.

## Техническое решение
Структура файлов, примеры кода, паттерны.

## Acceptance Criteria
- [ ] Конкретный критерий

## Тест-план
Какие тесты, как запускать.

## Зависимости
Что должно быть выполнено до.
```

`ai/PLAN.md` — только индекс со ссылками (< 50 строк). Содержательная часть — исключительно в файлах `ai/plans/`.
