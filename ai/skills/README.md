# ai/skills/ — канон скиллов

Скиллы — вызываемые через Skill-tool чек-листы/процедуры: грузятся **только при
вызове**, не висят в контексте каждой сессии (в отличие от `@`-import доков в
`CLAUDE.md`). Канон под git — здесь; live-копия для авто-дискавери Claude Code —
`.claude/skills/`.

**Синхронизация после правки канона** (обязательна):
```bash
cp -r ai/skills/* .claude/skills/
```
Новые скиллы становятся вызываемыми **только после рестарта** Claude Code.

## Скаффолды (генерация кода по конвенциям)

| Скилл | Источник | Назначение |
|-------|----------|------------|
| `new-spring-component` | coding.md | `@ConfigurationProperties` / `@Scheduled`-задача |
| `new-flyway-migration` | infra.md + architecture.md | миграция БД + JPA-слой (Entity/Repository/JpaStore/Mapper) |

## Процедуры

| Скилл | Источник | Назначение |
|-------|----------|------------|
| `review` | agents/ + testing.md | DoD-ревью перед merge (gate `./gradlew build` + judgment-субагенты) |

Детерминированный gate — **`./gradlew build`** (ArchUnit + Spotless + тесты);
read-only судагенты — [`ai/agents/`](../agents/README.md).

---

**Что НЕ перенесено из исходного проекта** (VPN-специфика, неприменимо к dev-crew):
скаффолды `new-bot-command`/`new-grafana-dashboard`/`new-alert-rule`/
`new-integration-client`/`new-templated-config`/`new-service-module`, процедуры
`vpn-troubleshoot`/`db-recovery`/`runner-setup`. Если для dev-crew понадобится
аналог (например, скилл под новый bounded context) — создавать с нуля по реальным
конвенциям, не адаптировать чужой.
