---
name: review
description: DoD-ревью изменений перед merge для dev-crew. Сначала гоняет детерминированный gate (./gradlew build — ArchUnit + Spotless + тесты + 100% покрытие), затем делает дешёвое inline-суждение и спавнит judgment-субагентов ТОЛЬКО по затронутому домену. Заменяет ручной прогон всех ревьюеров. Вызывать в конце задачи, перед merge.
---

# /Review — токен-экономный DoD-ревью

Единая точка входа фазы ревью. Принцип — **лестница стоимости**: детерминированное
ловит сборка (0 токенов модели), суждение — субагенты (дорого, только по триггеру).

## Шаг 1. Детерминированный Gate (всегда первым)

```bash
./gradlew build                                 # ArchUnit + Spotless + unit-тесты
./gradlew test -Dspring.profiles.active=tc      # если затронута БД/JPA (Testcontainers)
```

`build` закрывает детерминированное: правило слоёв (adapter→app→domain), стиль
(Google Java Format), покрытие веток. **Красный build = задача не завершена.**
Падения чинить до ревью-суждения.

## Шаг 2. Inline-суждение (в главном контексте, без спавна)

Дёшево, делаешь сам — НЕ спавнь субагента ради этого:

- **Тривиальный diff** (≤ ~5 файлов и без `*.java`): отревьюй инлайн полностью.
- **Judgment-крохи**:
  - commit body объясняет «почему», а не «что»; фича и рефактор не смешаны в одном
    коммите (CLAUDE.md §Коммит); тип/скоуп коммита верны;
  - `ai/PLAN.md` статус совпадает с git-реальностью;
  - Refactoring-фаза не rubber-stamp по смыслу.

## Шаг 3. Judgment-субагенты — ТОЛЬКО по затронутому домену

Спавнить через Task **только** релевантных и **только** если их домен в diff'е.
Передавай готовый контекст в промпт (`git diff main...HEAD --stat` + список файлов
+ «`Read` только изменённое, ≤ ~10 tool-вызовов»). Параллелить независимых можно.

| Изменение в diff'е | Субагент |
|---|---|
| `*.java` (нетривиально) | `java-reviewer` (оркестратор, сам помечает делегации) |
| auth/эндпоинты/ввод/секреты/PII + sandbox исполнения агента (IDOR, command injection, sandbox-побег) | `security-reviewer` |
| error-handling/`@Async`/`@Scheduled`/subprocess/`catch{}` | `silent-failure-hunter` |
| `*Test`/`*IT` или новые ветки production-кода | `test-reviewer` |
| границы bounded contexts, новые порты/адаптеры, зависимости контекстов | `architecture-reviewer` |
| `docker/**`, `*.gradle`, `db/migration/**`, `application*.yml` | `infra-reviewer` |
| Refactoring-фаза (touched code, DRY/SRP/dead code) | `refactor-cleaner` |

## Шаг 4. Перед merge

1. `./gradlew build` зелёный (+ `-Dspring.profiles.active=tc`, если трогали БД).
2. Findings субагентов закрыты или явно обоснованы.
3. Коммит по конвенции `type(scope): описание` (≤ 72 симв.), скоуп = bounded context.
4. Если правил канон агентов/скиллов в `ai/` — пересинхрон live-копий
   (`cp ai/agents/*.md .claude/agents/` / `cp -r ai/skills/* .claude/skills/`) и
   перезапуск Claude Code.
