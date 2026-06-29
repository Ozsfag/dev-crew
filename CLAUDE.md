# CLAUDE.md — Dev Crew

## Проект

Spring Boot 3.5 / Java 21. Оркестрирует команду ИИ-агентов через LangChain4j + Claude API.
Архитектор (человек) согласовывает планы; агенты выполняют задачи автономно.

**Группа**: `org.blacksoil` | **Порт**: 8081 | **БД**: PostgreSQL + Flyway

---

@ai/docs/architecture.md

@ai/docs/coding.md

@ai/docs/testing.md

@ai/docs/infra.md

---

## Workflow

### Перед каждой задачей
- Затрагивает 3+ файла или архитектуру → **использовать plan mode** (`/plan`)
- Прочитать все файлы, которые будут изменены, **до** написания кода

### Реализация (TDD — обязательно)
1. Написать падающий тест
2. Минимальная реализация → тест зелёный
3. Рефакторинг при необходимости

### После каждого изменения
```bash
./gradlew build          # ArchUnit + Spotless + тесты (обязательно)
./gradlew test -Dspring.profiles.active=tc   # если затронута БД или JPA
```
Задача не завершена, пока `build` красный.

### Коммит
```
type(scope): описание на английском  (≤ 72 символа)
```

| Тип        | Когда                                      |
|------------|--------------------------------------------|
| `feat`     | новая функциональность                     |
| `fix`      | исправление бага                           |
| `refactor` | изменение без смены поведения              |
| `test`     | тесты                                      |
| `chore`    | зависимости, конфиги сборки                |
| `docs`     | документация                               |

Скоуп = bounded context: `agent` `task` `auth` `audit` `organization` `notification` `billing` `common`.
Не смешивать рефакторинг и новую фичу в одном коммите.

### Ветка и PR
```
feat/scope-description
fix/scope-description
```
PR description: **Что изменилось** / **Почему** / **Как проверить** (чеклист с `./gradlew test`).

---

## Кастомные команды

| Команда  | Что делает                                         |
|----------|----------------------------------------------------|
| `/new`   | TDD-workflow для новой фичи                        |
| `/fix`   | Отладка: reproduce → isolate → fix → тест          |
| `/task`  | Создать файл плана для нового пункта roadmap       |
| `/review`| DoD-ревью перед merge (gate + judgment-субагенты)  |

---

## Code-review агенты

Read-only субагенты-ревьюеры в `ai/agents/` (канон) — спавнятся по затронутому
домену. Детерминированное закрывает **`./gradlew build`** (ArchUnit + Spotless +
тесты); агенты несут только judgment. Точка входа — Skill `/review`.

| Триггер в diff'е | Агент |
|------------------|-------|
| `*.java` (нетривиально) | `java-reviewer` (оркестратор делегаций) |
| auth/ввод/секреты/PII + sandbox исполнения агента | `security-reviewer` |
| `@Async`/`@Scheduled`/subprocess/`catch{}` | `silent-failure-hunter` |
| `*Test`/`*IT` или новые ветки | `test-reviewer` |
| границы bounded contexts, порты/адаптеры | `architecture-reviewer` |
| `docker/**`, `*.gradle`, `db/migration/**`, `application*.yml` | `infra-reviewer` |
| Refactoring-фаза (touched code) | `refactor-cleaner` |

Авто-дискавери: канон в `ai/agents/` (под git), live-копия в `.claude/agents/`.
Правил канон → `cp ai/agents/*.md .claude/agents/ && rm -f .claude/agents/README.md`
и перезапуск Claude Code. Подробнее — [ai/agents/README.md](ai/agents/README.md).

---

## Текущий фокус

@ai/PLAN.md

---

## Команды

```bash
./gradlew build
./gradlew test -Dspring.profiles.active=tc
./gradlew bootRun
./gradlew spotlessApply

docker compose -f docker/docker-compose.yml up -d
docker compose -f docker/docker-compose.yml -f docker/docker-compose.prod.yml up -d
```
