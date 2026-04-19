# Dev Crew — Roadmap

## Текущее состояние: П13 — Security Audit (в работе)

MVP готов. REST API, JWT-аутентификация, 5 агентов, мультитенантность (org/project),
биллинг, rate-limit recovery, Telegram-уведомления — реализованы.
П10–П12 выполнены. Проведён полный security audit — 6 CRITICAL, 3 HIGH, 5 MEDIUM уязвимостей.

## Roadmap

| ID    | Описание                                         | Статус | Файл                                                         |
|-------|--------------------------------------------------|--------|--------------------------------------------------------------|
| П7.1  | Тесты organization/                              | ✅     | [plans/p7.1](plans/p7.1-organization-tests.md)               |
| П7.2  | Тесты audit/ сервисов                            | ✅     | [plans/p7.2](plans/p7.2-audit-tests.md)                      |
| П7.3  | Stripe webhook реализация                        | ✅     | [plans/p7.3](plans/p7.3-stripe-webhook.md)                   |
| П7.4  | Fix Instant.now() в тестах                       | ✅     | [plans/p7.4](plans/p7.4-fix-instant-in-tests.md)             |
| П8    | Telegram Bot входящие + голос + pipeline         | ✅     | [plans/p8](plans/p8-telegram-bot-input.md)                   |
| П9    | IntelliJ IDEA Plugin                             | 🔲     | [plans/p9](plans/p9-intellij-plugin.md)                      |
| П10.1 | Безопасность и корректность                      | ✅     | [plans/p10.1](plans/p10.1-security.md)                       |
| П10.2 | Устранение архитектурных нарушений               | ✅     | [plans/p10.2](plans/p10.2-architecture.md)                   |
| П10.3 | Качество кода: DRY / SRP                         | ✅     | [plans/p10.3](plans/p10.3-code-quality.md)                   |
| П10.4 | API: пагинация (OpenAPI/SSE отложены)            | ✅     | [plans/p10.4](plans/p10.4-api.md)                            |
| П10.5 | Observability: метрики и трейсинг                | ✅⚠️   | [plans/p10.5](plans/p10.5-observability.md) — 5.2 тест, 5.3 отложено |
| П10.6 | Инфраструктура: Docker и CI/CD                   | ✅     | [plans/p10.6](plans/p10.6-infrastructure.md)                 |
| П10.7 | База данных: схема и производительность          | ✅⚠️   | [plans/p10.7](plans/p10.7-database.md) — 7.2 не выполнено, 7.3 API-фильтр |
| П10.8 | Тесты: пробелы в покрытии                        | ✅     | [plans/p10.8](plans/p10.8-test-coverage.md)                  |
| П11   | Code Review: безопасность и корректность         | ✅     | [plans/p11](plans/p11-code-review-findings.md)               |
| П12   | Незакрытые пробелы из ревизии (тесты + фичи)    | 🔲     | [plans/p12](plans/p12-gaps-from-revision.md)                 |
| П13   | Security Audit: 6 CRITICAL + 3 HIGH + 5 MEDIUM  | ✅⚠️   | [plans/p13](plans/p13-security-audit.md) — IDOR, пароль, Stripe secret, prod-логи |
| П14   | Security Fixes: rate limit, CORS, SSL, idempotency | 🔲  | [plans/p14](plans/p14-security-fixes.md)                     |
