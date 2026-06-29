# П18 — Миграция stripe-java 28.3 → 33.x

**Статус:** 🔲 Запланировано
**Источник:** dependabot PR #12 (28.3.0 → 33.1.0) закрыт — Unit-тесты падали (реальная
поломка API биллинга, не bump).

---

## Контекст

Биллинг использует Stripe Java SDK 28.3.0. Dependabot предложил скачок на 33.1.0 (#12) —
~5 мажоров, `./gradlew test` упал на компиляции. Stripe между этими версиями менял API
(в т.ч. service-based `StripeClient`, билдеры параметров, сигнатуры методов ресурсов).

Если не делать: остаёмся на 28.3. Риск — отставание от актуального Stripe API
(новые версии API биллинга, депрекации на стороне Stripe, security-фиксы SDK).

## Проблема

Прямой bump ломает компиляцию в bounded context `billing`. Затронутые файлы:

- `billing/adapter/in/stripe/StripeWebhookAdapter.java` — приём webhook,
  **верификация подписи** (`Webhook.constructEvent(payload, sigHeader, secret)`).
- `billing/app/service/command/StripeWebhookService.java` — разбор `Event` и
  десериализация объектов события (в новых версиях изменился доступ к
  `EventDataObjectDeserializer`/типам).
- `billing/app/service/command/StripeIdempotencyService.java`,
  `billing/app/config/BillingProperties.java` — конфиг ключей/секретов.
- `billing/domain/StripeWebhookPort.java` и persistence обработанных событий
  (`StripeProcessedEvent*`) — контракт portа может не меняться, но проверить.

## Техническое решение

1. Отдельная ветка `feat/stripe-java-33`.
2. `build.gradle`: `com.stripe:stripe-java:33.x`.
3. Пройтись по Stripe-migration-changelog для мажоров 29→33; типичные правки:
   - конструкция клиента/вызовов ресурсов (возможен переход на `StripeClient`),
   - билдеры параметров (`*CreateParams.builder()...`),
   - доступ к объекту события через `EventDataObjectDeserializer`.
4. **Сохранить инварианты безопасности** (из security-аудита): верификация подписи
   webhook остаётся constant-time/fail-closed; идемпотентность по event-id не сломана.
5. Итеративно `./gradlew build`.

## Acceptance Criteria

- [ ] `./gradlew build` зелёный.
- [ ] `./gradlew test -Dspring.profiles.active=tc` зелёный.
- [ ] Верификация подписи Stripe-webhook работает; невалидная подпись → fail-closed (4xx).
- [ ] Идемпотентность: повторная доставка event-id не удваивает учёт.

## Тест-план

Unit-тесты `StripeWebhookService`/`StripeWebhookAdapter` (валидная/невалидная подпись,
дубль event-id) — обновить под новый API, сохранив сценарии. TDD при смене поведения.
Запуск: `./gradlew build` + `-Dspring.profiles.active=tc`.

## Refactoring

Мини-фаза (touched code only): [`refactor-cleaner`](../agents/refactor-cleaner.md) по
изменённым Stripe-файлам; убрать обёртки/обходы старого API.

## Review (Skill /review)

`/review`: `./gradlew build` + `java-reviewer` + `security-reviewer` (webhook-подпись,
fail-closed, секреты — приоритет) + `test-reviewer`. Минимум: build + security-reviewer.

## Зависимости

Независима от П17. Можно делать в любом порядке.
