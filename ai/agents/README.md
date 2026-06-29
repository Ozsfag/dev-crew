# ai/agents — проектные субагенты для ревью

Определения специализированных code-review субагентов, адаптированные под
конвенции **этого** проекта (CLAUDE.md + ai/docs). Это инертный markdown —
кода не исполняют; описывают роль и чеклист, по которому Claude ревьюит изменения.

Происхождение: адаптация набора ревьюеров из другого Spring-проекта. Выкинута
multimodule/Reactor/OAuth2/xray-специфика, вставлены реалии dev-crew —
гексагональная архитектура, claude-CLI как ядро исполнения агентов, JWT,
Stripe, Telegram, MapStruct/Lombok, ArchUnit, Testcontainers, 100% покрытие.

## Детерминированный gate — это `./gradlew build`

В отличие от исходного проекта (там были bash-скрипты `scripts/review/`), у
dev-crew детерминированную механику закрывает **`./gradlew build`**:

| Проверка                       | Чем закрыта                          |
|--------------------------------|--------------------------------------|
| Архитектурные правила (слои)   | ArchUnit                             |
| Форматирование/стиль           | Spotless + Google Java Format        |
| Тесты + покрытие веток         | JUnit (+ Testcontainers с `-Dspring.profiles.active=tc`) |

Поэтому субагенты **не дублируют** то, что ловит `build`: их зона — **judgment**,
который не берёт компилятор/ArchUnit/grep. Каждый промпт это явно оговаривает.

## Агенты

| Файл                       | Когда использовать                                                                       |
|----------------------------|------------------------------------------------------------------------------------------|
| `java-reviewer.md`         | Первичный pass после любых изменений в `*.java` перед merge. Spring/JPA-специфика + оркестрация делегаций. |
| `security-reviewer.md`     | Изменения в auth/эндпоинтах/вводе/секретах **и в исполнении агентов** (claude-CLI sandbox, command injection). Threat model dev-crew. |
| `silent-failure-hunter.md` | Проглоченные исключения, `.get()` на Optional, отсутствие MDC `taskId`, потеря ошибки в `@Async`-пути. |
| `refactor-cleaner.md`      | Обязательная Refactoring-фаза. DRY/SRP/dead code/identity-`@Mapping` по touched code.    |
| `test-reviewer.md`         | Изменения в `*Test`/`*IT` или новые ветки production-кода. Полнота покрытия (100%), Mockito/MockMvc/Testcontainers, каждый `case`/`catch`. |
| `architecture-reviewer.md` | Изменения на границах bounded contexts, новые порты/адаптеры, зависимости между контекстами. Циклы, dependency-rule, SOLID. |
| `infra-reviewer.md`        | Изменения в `docker/**`, `*.gradle`, `db/migration/**`, `application*.yml`. Docker-compose, Flyway, профили, sandbox-конфиг. |

Все семь — **read-only** (`Read/Grep/Glob/Bash`): сообщают findings, не правят код.

## Связь между агентами

`java-reviewer` — оркестратор первичного pass'а по `*.java`. Он явно делегирует:
`security-reviewer` (auth/PII/sandbox), `silent-failure-hunter` (swallowed errors),
`test-reviewer` (покрытие), `architecture-reviewer` (границы контекстов),
`refactor-cleaner` (touched-code refactor). `infra-reviewer` запускается по
инфра-файлам независимо. Делегация — **пометка в выводе**: под-агентов спавнит
основной агент, не вложенный спавн (стоимости перемножаются).

Точка входа — Skill **`/review`** ([ai/skills/review/SKILL.md](../skills/review/SKILL.md)):
гоняет `./gradlew build`, делает inline-суждение, спавнит judgment-агентов только
по затронутому домену.

## Токен-эффективность (важно)

Субагент стартует «с холода» и пересылает контекст на каждом шаге (рост ~`N²` от
tool-вызовов):

- **Не спавнить на мелочь**: диф ≤ ~5 файлов без `*.java` — ревьюить инлайн.
- **Передавать готовый контекст** в промпт спавна: `git diff main...HEAD --stat`
  + список файлов + «`Read` только изменённое, `grep -n` вместо полного чтения,
  ≤ ~10 tool-вызовов».
- **Детерминированное — в `./gradlew build`**, не в субагент.
- **Right-size модель** в frontmatter: `haiku` механическим, `sonnet` судящим.
- **Короткие прогоны** (< 5 мин → тёплый prompt-cache); итерация — `SendMessage`
  в ту же сессию, не новый холодный спавн.
- **Locate** («где лежит») — `grep`/`Glob` инлайн или `Explore`-агент, **не** reviewer.

## Как подключить (авто-дискавери Claude Code)

Claude Code находит субагенты в `.claude/agents/` или `~/.claude/agents/`. Канон
живёт здесь (`ai/agents/`, под git); для авто-дискавери — скопировать:

```bash
mkdir -p .claude/agents
cp ai/agents/*.md .claude/agents/
rm -f .claude/agents/README.md     # README — не агент
```

Обновил канон в `ai/agents/` → повтори копирование и перезапусти Claude Code.

## Карта «док → агент» (каждый review-shaped док — свой владелец)

`architecture.md` → architecture-reviewer · `coding.md` → java-reviewer ·
`testing.md` → test-reviewer · `infra.md` → infra-reviewer ·
security (threat model) → security-reviewer.
