# П11 — Code Review: Найденные проблемы

## Контекст

Code review всего проекта (апрель 2026). Выявлены проблемы безопасности и корректности,
не покрытые предыдущими планами П10.x.

---

## 11.1 ProcessBuilderCommandRunner: нет таймаута — CRITICAL

**Файл**: `agent/adapter/out/llm/process/ProcessBuilderCommandRunner.java:21`

**Проблема**: `process.waitFor()` блокирует поток бесконечно. Зависший процесс (например,
`gradle test` при зависшем тесте) удерживает VirtualThread навсегда. При нескольких параллельных
задачах — исчерпание пула.

**Решение**:
```java
var timedOut = !process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
if (timedOut) {
  process.destroyForcibly();
  return new CommandResult(-1, "TIMEOUT: процесс превысил лимит " + timeoutSeconds + "с");
}
```

Добавить `command-timeout-seconds: 300` в `AgentProperties` (или `SandboxProperties`).

**Acceptance Criteria**:
- [x] `ProcessBuilderCommandRunner.run()` завершается через не более `timeoutSeconds` секунд
- [x] Зависший процесс принудительно уничтожается (`destroyForcibly`)
- [x] Таймаут вынесен в конфиг (`devcrew.agent.command-timeout-seconds: 300`)
- [ ] Тест: `run_returns_timeout_result_when_process_exceeds_limit`

---

## 11.2 SandboxPolicy: симлинк-атака через normalize() — HIGH

**Файл**: `agent/app/policy/SandboxPolicy.java:28`

**Проблема**: `Path.of(path).normalize().toAbsolutePath()` не разрешает символические ссылки.
Атака: создать `/projects/app/evil -> /etc/passwd`. После `normalize()` путь выглядит
корректным (`/projects/app/evil`), проверка проходит, но реальный файл — вне sandbox.

**Решение**:
```java
Path normalized;
try {
  normalized = Path.of(path).toRealPath(); // разрешает симлинки
} catch (NoSuchFileException e) {
  // файл ещё не существует — нормализуем без разрешения симлинков
  normalized = Path.of(path).normalize().toAbsolutePath();
} catch (IOException e) {
  throw new DomainException("Не удалось проверить путь: " + path);
}
```

**Acceptance Criteria**:
- [x] `SandboxPolicy` использует `toRealPath()` для существующих путей
- [x] Симлинк вне sandbox правильно отклоняется
- [ ] Тест: `validatePath_throws_when_symlink_points_outside_sandbox`
- [ ] Тест: `validatePath_allows_valid_nonexistent_path_inside_sandbox`

---

## 11.3 JWT: небезопасный дефолтный секрет — MEDIUM

**Файл**: `src/main/resources/application.yml:54`

**Проблема**: `${DEVCREW_AUTH_JWT_SECRET:change-me-min-32-chars-long-secret!!}` — если env
переменная не задана, приложение запускается с предсказуемым секретом. Злоумышленник может
генерировать валидные JWT-токены.

**Решение**: Убрать fallback-значение — приложение должно падать при старте без секрета:
```yaml
secret: ${DEVCREW_AUTH_JWT_SECRET}
```

Добавить валидацию в `JwtProperties`:
```java
@NotBlank
@Size(min = 32)
private String secret;
```

**Acceptance Criteria**:
- [x] `application.yml` не содержит fallback-секрета
- [x] `JwtProperties.secret` помечен `@NotBlank @Size(min=32)`
- [x] Приложение не запускается если `DEVCREW_AUTH_JWT_SECRET` не задан
- [ ] Тест: `JwtPropertiesValidationTest` проверяет отказ при коротком секрете

---

## 11.4 Actuator: публичный доступ к /actuator/prometheus — MEDIUM

**Файл**: `bootstrap/SecurityConfig.java:37-38`

**Проблема**: `/actuator/**` открыт без аутентификации. Метрики Prometheus раскрывают:
имена задач, роли агентов, количество запросов, счётчики ошибок — ценная информация
для разведки.

**Решение**: ограничить доступ IP-адресом scraper'а или требовать аутентификацию:
```java
.requestMatchers("/actuator/health").permitAll()  // для load balancer
.requestMatchers("/actuator/**").hasRole("ARCHITECT")  // или IP-based
```

Либо через `management.endpoints.web.exposure.include: health` (убрать `prometheus, info`
из публичного доступа в `application.yml` и создать отдельный management-порт).

**Acceptance Criteria**:
- [x] `/actuator/prometheus` недоступен без аутентификации
- [x] `/actuator/health` остаётся публичным (нужен для probe)
- [ ] Тест: `GET /actuator/prometheus` возвращает 401 без токена

---

## 11.5 BillingPostAgentHook: неустранённый N+1 (П10.2.5 не завершён) — LOW

**Файл**: `billing/adapter/in/hook/BillingPostAgentHook.java:29`

**Проблема**: `onAgentCompleted()` вызывает `organizationQueryService.getProjectById(projectId)`
для получения `orgId`. П10.2.5 ставил задачу передавать `orgId` напрямую через сигнатуру
`PostAgentHook`, но была передана только половина — `projectId` добавлен, `orgId` — нет.

**Решение**: изменить сигнатуру `PostAgentHook`:
```java
public interface PostAgentHook {
  void onAgentCompleted(UUID taskId, UUID projectId, UUID orgId, AgentRole role, String result);
}
```

`AgentExecutionService` берёт `orgId` из `TaskModel.orgId()` (поле уже есть в модели).
`BillingPostAgentHook` убирает `OrganizationQueryService` зависимость.

**Acceptance Criteria**:
- [x] `PostAgentHook.onAgentCompleted` принимает `orgId`
- [x] `BillingPostAgentHook` не инжектирует `OrganizationQueryService`
- [x] `AuditPostAgentHook` также обновлён
- [x] Все тесты хуков обновлены

---

## 11.6 TaskJpaStore: отсутствие ограничения размера страницы — LOW

**Файл**: `task/adapter/out/persistence/store/TaskJpaStore.java:56`

**Проблема**: `findByOrgId(UUID orgId, int page, int size)` принимает `size` без верхней границы.
Клиент может запросить `size=100000`, что вызовет загрузку всей таблицы в память.

**Решение**:
```java
private static final int MAX_PAGE_SIZE = 100;

@Override
public PageResult<TaskModel> findByOrgId(UUID orgId, int page, int size) {
  var effectiveSize = Math.min(size, MAX_PAGE_SIZE);
  var pageable = PageRequest.of(page, effectiveSize, Sort.by(Sort.Direction.DESC, "createdAt"));
  // ...
}
```

Аналогично в `AuditJpaStore`.

**Acceptance Criteria**:
- [x] `TaskJpaStore` ограничивает `size` до 100
- [x] `AuditJpaStore` ограничивает `size` до 100
- [ ] Тест: запрос с `size=99999` возвращает не более 100 записей

---

## 11.7 Нет эндпоинта выхода / отзыва refresh-токена — LOW

**Файл**: `auth/app/service/AuthService.java`

**Проблема**: `RefreshTokenModel` имеет поле `revoked`, но нет эндпоинта `POST /api/auth/logout`
для его установки. Скомпрометированный refresh-токен живёт 7 дней без возможности отзыва.

**Решение**:
```java
// POST /api/auth/logout
// Принимает refresh-токен, устанавливает revoked=true
public void logout(String rawRefreshToken) {
  var hash = jwtService.hashToken(rawRefreshToken);
  var token = refreshTokenStore.findByTokenHash(hash)
      .orElseThrow(() -> new AuthException("Токен не найден"));
  refreshTokenStore.save(token.withRevoked(true));
}
```

**Acceptance Criteria**:
- [x] `POST /api/auth/logout` принимает refresh-токен и отзывает его
- [x] Отозванный токен не принимается в `POST /api/auth/refresh`
- [x] Тест: `logout_revokes_refresh_token` + `refresh_fails_with_revoked_token`

---

## 11.8 Отсутствуют security headers — LOW

**Файл**: `bootstrap/SecurityConfig.java`

**Проблема**: нет `X-Frame-Options`, `X-Content-Type-Options`, `Strict-Transport-Security`.
Spring Security по умолчанию добавляет некоторые заголовки, но не все.

**Решение** (добавить в `filterChain`):
```java
.headers(headers -> headers
    .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
    .contentTypeOptions(Customizer.withDefaults())
    .httpStrictTransportSecurity(hsts -> hsts
        .includeSubDomains(true)
        .maxAgeInSeconds(31536000)))
```

**Acceptance Criteria**:
- [x] Ответы содержат `X-Frame-Options: DENY`
- [x] Ответы содержат `X-Content-Type-Options: nosniff`
- [x] Ответы содержат `Strict-Transport-Security`

---

## Приоритет выполнения

| ID   | Приоритет | Трудоёмкость |
|------|-----------|--------------|
| 11.1 | Critical  | 2ч           |
| 11.2 | High      | 1ч           |
| 11.3 | Medium    | 30мин        |
| 11.4 | Medium    | 30мин        |
| 11.5 | Low       | 1ч           |
| 11.6 | Low       | 30мин        |
| 11.7 | Low       | 2ч           |
| 11.8 | Low       | 30мин        |

## Тест-план

```bash
./gradlew spotlessApply
./gradlew test
```
