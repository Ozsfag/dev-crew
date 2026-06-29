# П17 — Миграция на Spring Boot 4

**Статус:** 🔲 Запланировано
**Источник:** dependabot PR #11 (3.5.0 → 4.1.0) закрыт — Unit-тесты падали (реальная
поломка API, не bump).

---

## Контекст

dev-crew на Spring Boot 3.5.0. Spring Boot 4 = Spring Framework 7: подняты baseline
(Java 17+, Jakarta EE 11), удалён ряд deprecated-API, переработан Spring Security 7.
Dependabot предложил 3.5.0 → 4.1.0 (#11) — `./gradlew test` упал на компиляции/тестах.

Если не делать: остаёмся на 3.5.x. Это не блокер (3.5 поддерживается), но со временем
копится разрыв: новые версии Stripe/jjwt/Testcontainers начнут требовать SF7, а security-
фиксы пойдут в первую очередь в 4.x.

## Проблема

Прямой bump ломает сборку. Точечные зоны риска (проверить по `./gradlew build`):

- **Spring Security 7** — основной риск. `bootstrap/SecurityConfig.java`: API
  `SecurityFilterChain`/`HttpSecurity` в SF7 изменён (lambda-DSL обязателен, удалены
  устаревшие методы). JWT-фильтр на jjwt — проверить совместимость с новым security-контекстом.
- **Jakarta / validation** — `jakarta.validation` в `@RequestBody`-DTO; возможны изменения
  в bean-validation провайдере.
- **MockMvc standalone** — тесты контроллеров (`MockMvcBuilders.standaloneSetup`) —
  проверить совместимость API в SF7.
- **Actuator / Micrometer** — endpoint `health, prometheus`; возможны изменения
  exposure-конфига.
- **Сторонние**: Bucket4j 8.10, Caffeine, MapStruct 1.6.3, jjwt 0.13, stripe — проверить
  матрицу совместимости с SF7 (часть может потребовать своих bump'ов).

## Техническое решение

1. Отдельная ветка `feat/spring-boot-4`. Java 21 ✅ (SF7 требует 17+), Gradle 9.6.1 ✅.
2. `build.gradle`: `id 'org.springframework.boot' version '4.x'` +
   `io.spring.dependency-management` совместимой версии.
3. Итеративно `./gradlew build`, чинить по областям выше — **в первую очередь**
   `SecurityConfig` (SF7 security DSL).
4. Свериться с официальным Spring Boot 4 Migration Guide по каждому красному месту.
5. Поднять при необходимости транзитивные зависимости до SF7-совместимых.

## Acceptance Criteria

- [ ] `./gradlew build` зелёный (ArchUnit + Spotless + unit).
- [ ] `./gradlew test -Dspring.profiles.active=tc` зелёный (интеграционные).
- [ ] Приложение стартует (`./gradlew bootRun`), `/actuator/health` отвечает.
- [ ] JWT-аутентификация и мультитенантные проверки работают (auth/billing эндпоинты).
- [ ] Образ собирается (`build-and-push`).

## Тест-план

Существующие тесты — регрессионная сеть. TDD: при изменении поведения security/валидации
сначала поправить/добавить тест. Запуск: `./gradlew build` + `-Dspring.profiles.active=tc`.
Особое внимание — тесты `SecurityConfig`/контроллеров (standalone MockMvc).

## Refactoring

Мини-фаза (touched code only): прогнать [`refactor-cleaner`](../agents/refactor-cleaner.md)
по изменённым под SF7 файлам; убрать deprecated-обходы, оставшиеся после миграции.

## Review (Skill /review)

`/review`: `./gradlew build` + `java-reviewer` (Spring-механика) + `security-reviewer`
(SecurityConfig/JWT — приоритет) + `test-reviewer`. Минимум: build + security-reviewer.

## Зависимости

Нет жёстких. Gradle 9 (П-выполнено) уже на месте. Делать **до** или независимо от П18.
