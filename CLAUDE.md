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
