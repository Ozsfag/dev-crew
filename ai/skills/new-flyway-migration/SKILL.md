---
name: new-flyway-migration
description: Добавить Flyway-миграцию + JPA-сущность/репозиторий/маппер для dev-crew по конвенциям (V<N>__*.sql, ddl-auto validate, MapStruct-маппинг, три слоя). Вызывать при изменении схемы БД. Экономит перечитывание infra.md.
---

# /new-flyway-migration — миграция схемы БД

Канон — [infra.md §«Миграции БД»](../../docs/infra.md). `ddl-auto: validate` —
схему меняет ТОЛЬКО Flyway, никогда Hibernate.

## 1. Миграционный файл

- Путь: `src/main/resources/db/migration/V<N>__<slug>.sql`.
- `V<N>` уникальна и монотонна (следующий свободный номер). Дубль номера → Flyway
  падает на старте.
- Любая новая таблица/колонка/индекс **обязательно** через миграцию — без неё
  Hibernate `validate` уронит контекст на старте.

## 2. JPA-слой (если новая таблица/колонка) — три слоя

Соблюдать гексагональное размещение (architecture.md):
- `@Entity` → `adapter/out/persistence/entity/` (`*Entity`).
- `JpaRepository` → `adapter/out/persistence/repository/` (`*Repository`).
- `*JpaStore` (реализация domain-порта `*Store`) → `adapter/out/persistence/store/`.
- `*PersistenceMapper` (Entity↔Model) → `adapter/out/persistence/mapper/` —
  **только MapStruct** (`componentModel = "spring"`), без identity-`@Mapping`,
  ручная конвертация запрещена.
- Domain-`*Model` — record в `domain/model/`; порт `*Store` — в `domain/store/`,
  возвращает `PageResult<T>` (не Spring `Page`).

## 3. Тест + review

- Roundtrip-тест стора — `extends IntegrationTestBase` (`@SpringBootTest` +
  `@ActiveProfiles("tc")` + `@Transactional` + `@Tag("integration")`):
  ```bash
  ./gradlew test -Dspring.profiles.active=tc
  ```
- Сервисная логика на новой колонке — покрыть каждую ветку (100%).
- Review: `java-reviewer` + `test-reviewer` (новые ветки) + `infra-reviewer`
  (миграция/симметрия Entity↔Flyway).
