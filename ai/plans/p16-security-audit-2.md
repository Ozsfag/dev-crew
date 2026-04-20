# П16 — Security Audit #2: новые уязвимости

**Дата аудита:** 2026-04-20
**Статус:** 🔲 Не выполнено

---

## Итоговая картина

| Критичность | Кол-во |
|-------------|--------|
| CRITICAL    | 3      |
| HIGH        | 4      |
| MEDIUM      | 3      |

---

## CRITICAL

### П16.1 IDOR: GET /api/billing/usage без проверки org

**Файл:** `billing/adapter/in/web/controller/BillingController.java`

**Проблема:** `orgId` принимается как `@RequestParam`. Любой аутентифицированный пользователь
может получить данные об использовании и расходах **любой** другой организации.

```java
// Текущий код — уязвим:
@GetMapping("/usage")
public ResponseEntity<UsageSummaryResponse> getUsage(
    @RequestParam UUID orgId, @RequestParam String month) {
  var summary = usageQueryService.getMonthlySummary(orgId, yearMonth);
  ...
}
```

**Исправление:** убрать `orgId` из параметров — брать из `currentUser`:

```java
@GetMapping("/usage")
public ResponseEntity<UsageSummaryResponse> getUsage(
    @RequestParam String month,
    @AuthenticationPrincipal AuthenticatedUser currentUser) {
  var yearMonth = YearMonth.parse(month);
  var summary = usageQueryService.getMonthlySummary(currentUser.orgId(), yearMonth);
  return ResponseEntity.ok(mapper.toResponse(summary));
}
```

**Тесты:**
- `GET_usage_uses_current_user_org_id`
- `GET_usage_returns_200_with_summary`

---

### П16.2 IDOR: GET /api/organizations/{id} без проверки ownership

**Файл:** `organization/adapter/in/web/controller/OrganizationController.java`

**Проблема:** `getById(@PathVariable UUID id)` не проверяет принадлежность. Любой пользователь
может получить имя, repoPath и другие данные чужой организации.

```java
// Текущий код — уязвим:
@GetMapping("/{id}")
public OrganizationResponse getById(@PathVariable UUID id) {
  return mapper.toResponse(queryService.getById(id));
}
```

**Исправление:**

```java
@GetMapping("/{id}")
public OrganizationResponse getById(
    @PathVariable UUID id,
    @AuthenticationPrincipal AuthenticatedUser currentUser) {
  if (!id.equals(currentUser.orgId())) {
    throw new ForbiddenException("Нет доступа к этой организации");
  }
  return mapper.toResponse(queryService.getById(id));
}
```

**Тест:**
- `GET_organizations_id_returns_403_when_wrong_org`

---

### П16.3 IDOR: POST /api/organizations/{orgId}/projects без проверки ownership

**Файл:** `organization/adapter/in/web/controller/OrganizationController.java`

**Проблема:** `createProject(@PathVariable UUID orgId, ...)` принимает `orgId` из пути без
проверки. Злоумышленник может создавать проекты в **чужой** организации.

```java
// Текущий код — уязвим:
@PostMapping("/{orgId}/projects")
@ResponseStatus(HttpStatus.CREATED)
public ProjectResponse createProject(
    @PathVariable UUID orgId, @Valid @RequestBody CreateProjectRequest request) {
  return mapper.toResponse(commandService.createProject(orgId, ...));
}
```

**Исправление:**

```java
@PostMapping("/{orgId}/projects")
@ResponseStatus(HttpStatus.CREATED)
public ProjectResponse createProject(
    @PathVariable UUID orgId,
    @Valid @RequestBody CreateProjectRequest request,
    @AuthenticationPrincipal AuthenticatedUser currentUser) {
  if (!orgId.equals(currentUser.orgId())) {
    throw new ForbiddenException("Нет доступа к этой организации");
  }
  return mapper.toResponse(
      commandService.createProject(orgId, request.name(), request.repoPath()));
}
```

**Тест:**
- `POST_organizations_orgId_projects_returns_403_when_wrong_org`

---

## HIGH

### П16.4 Rate limiting отсутствует на /api/auth/refresh

**Файл:** `auth/adapter/in/web/controller/AuthController.java`,
`auth/app/service/AuthRateLimitService.java`

**Проблема:** `/login` (10/15 мин) и `/register` (5/час) защищены, но `/refresh` — нет.
Украденный refresh-токен можно эксплуатировать неограниченно.

**Исправление** — добавить в `AuthRateLimitService`:

```java
private static final int REFRESH_LIMIT = 20;

public void checkRefreshAttempt(String tokenPrefix) {
  // Используем первые 16 символов токена как ключ (не весь токен — не логируем)
  var key = "refresh:" + tokenPrefix.substring(0, Math.min(16, tokenPrefix.length()));
  var bucket = buckets.computeIfAbsent(key, k -> refreshBucket());
  if (!bucket.tryConsume(1)) {
    throw new TooManyRequestsException("Слишком много попыток обновления токена.");
  }
}

private static Bucket refreshBucket() {
  return Bucket.builder()
      .addLimit(Bandwidth.builder()
          .capacity(REFRESH_LIMIT)
          .refillGreedy(REFRESH_LIMIT, Duration.ofMinutes(5))
          .build())
      .build();
}
```

В `AuthController.refresh()`:

```java
@PostMapping("/refresh")
public RefreshResponse refresh(@Valid @RequestBody RefreshRequest request) {
  rateLimitService.checkRefreshAttempt(request.refreshToken());
  return mapper.toResponse(authService.refresh(request.refreshToken()));
}
```

**Тесты:**
- `checkRefreshAttempt_throws_after_20_attempts`
- `POST_refresh_returns_429_when_rate_limited`

---

### П16.5 Отсутствие @Size на входящих DTO — DoS через большие payload

**Файлы:**
- `auth/adapter/in/web/dto/LoginRequest.java`
- `auth/adapter/in/web/dto/RefreshRequest.java`
- `auth/adapter/in/web/dto/LogoutRequest.java`
- `organization/adapter/in/web/dto/CreateOrganizationRequest.java`
- `organization/adapter/in/web/dto/CreateProjectRequest.java`
- `task/adapter/in/web/dto/CreateTaskRequest.java`

**Проблема:** Нет ограничений на длину строк. Злоумышленник может отправить строки в несколько
МБ, загружая память JVM и замедляя валидацию.

**Исправление:**

```java
// LoginRequest.java
public record LoginRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(max = 255) String password) {}

// RefreshRequest.java
public record RefreshRequest(@NotBlank @Size(max = 2048) String refreshToken) {}

// LogoutRequest.java
public record LogoutRequest(@NotBlank @Size(max = 2048) String refreshToken) {}

// CreateOrganizationRequest.java
public record CreateOrganizationRequest(@NotBlank @Size(max = 255) String name) {}

// CreateProjectRequest.java
public record CreateProjectRequest(
    @NotBlank @Size(max = 255) String name,
    @Size(max = 2048) String repoPath) {}

// CreateTaskRequest.java
public record CreateTaskRequest(
    @NotBlank @Size(max = 500) String title,
    @NotBlank @Size(max = 20000) String description,
    @NotNull AgentRole role,
    UUID projectId) {}
```

Также добавить глобальное ограничение в `application.yml`:

```yaml
server:
  tomcat:
    max-http-form-post-size: 2MB
    max-swallow-size: 2MB
  max-http-request-header-size: 16KB
```

**Тесты:**
- `POST_login_returns_400_when_email_too_long`
- `POST_tasks_returns_400_when_title_too_long`
- `POST_tasks_returns_400_when_description_too_long`

---

### П16.6 repoPath не валидируется — path traversal через CreateProjectRequest

**Файл:** `organization/adapter/in/web/dto/CreateProjectRequest.java`

**Проблема:** `repoPath` принимается без проверки. Значение `../../etc/passwd` или
`/etc/shadow` передаётся напрямую в агент как рабочая директория.

**Исправление:**

```java
public record CreateProjectRequest(
    @NotBlank @Size(max = 255) String name,
    @Size(max = 2048)
    @Pattern(
        regexp = "^(/[a-zA-Z0-9._-]+)+/?$|^$",
        message = "repoPath должен быть абсолютным путём без '..' и специальных символов")
    String repoPath) {}
```

Дополнительно в `OrganizationCommandService.createProject()` использовать `SandboxPolicy`
для проверки пути если он не пустой:

```java
if (repoPath != null && !repoPath.isBlank()) {
  sandboxPolicy.validatePath(repoPath);
}
```

**Тест:**
- `POST_organizations_orgId_projects_returns_400_when_repoPath_traversal`

---

### П16.7 Отсутствует Content-Security-Policy заголовок

**Файл:** `bootstrap/SecurityConfig.java`

**Проблема:** HSTS и X-Frame-Options настроены, но CSP отсутствует. Для REST API
минимальный CSP всё равно нужен — он ограничивает поверхность атаки для клиентов,
обрабатывающих JSON-ответы.

**Исправление** — добавить в `.headers(...)`:

```java
.headers(
    headers ->
        headers
            .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
            .contentTypeOptions(Customizer.withDefaults())
            .contentSecurityPolicy(
                csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
            .httpStrictTransportSecurity(
                hsts ->
                    hsts.includeSubDomains(true)
                        .maxAgeInSeconds(31536000)
                        .preload(true)))
```

**Тест:**
- `GET_api_returns_csp_header`

---

## MEDIUM

### П16.8 AuthRateLimitService: ConcurrentHashMap растёт бесконечно

**Файл:** `auth/app/service/AuthRateLimitService.java`

**Проблема:** `ConcurrentHashMap<String, Bucket>` никогда не очищается. Атакующий,
генерируя уникальные email'ы, может заполнить heap JVM — OOM и падение приложения.

**Исправление** — заменить на Caffeine с TTL:

```java
// build.gradle — уже есть Caffeine в classpath через Spring Boot Cache
// Добавить:
implementation 'com.github.ben-manes.caffeine:caffeine'
```

```java
// AuthRateLimitService.java
import com.github.ben-manes.caffeine.cache.Cache;
import com.github.ben-manes.caffeine.cache.Caffeine;

@Service
public class AuthRateLimitService {

  private static final int LOGIN_LIMIT   = 10;
  private static final int REGISTER_LIMIT = 5;
  private static final int REFRESH_LIMIT  = 20;

  // Записи живут 1 час после последнего доступа → автоматически удаляются
  private final Cache<String, Bucket> buckets =
      Caffeine.newBuilder()
          .expireAfterAccess(1, TimeUnit.HOURS)
          .maximumSize(100_000)       // жёсткий потолок: 100K уникальных ключей
          .build();

  public void checkLoginAttempt(String email) {
    var bucket = buckets.get("login:" + email, k -> loginBucket());
    if (!bucket.tryConsume(1)) {
      throw new TooManyRequestsException("Слишком много попыток входа. Повторите через 15 минут.");
    }
  }

  public void checkRegisterAttempt(String email) {
    var bucket = buckets.get("register:" + email, k -> registerBucket());
    if (!bucket.tryConsume(1)) {
      throw new TooManyRequestsException("Слишком много попыток регистрации. Повторите позже.");
    }
  }

  public void checkRefreshAttempt(String tokenPrefix) {
    var key = "refresh:" + tokenPrefix.substring(0, Math.min(16, tokenPrefix.length()));
    var bucket = buckets.get(key, k -> refreshBucket());
    if (!bucket.tryConsume(1)) {
      throw new TooManyRequestsException("Слишком много попыток обновления токена.");
    }
  }

  private static Bucket loginBucket() { ... }
  private static Bucket registerBucket() { ... }
  private static Bucket refreshBucket() { ... }
}
```

**Тест:**
- `checkLoginAttempt_does_not_grow_map_unboundedly` (проверить maximumSize через Caffeine stats)

---

### П16.9 Actuator: /actuator/info открыт для всех аутентифицированных (избыточно)

**Файл:** `src/main/resources/application.yml`

**Проблема:** `info` включён в `exposure.include`. Endpoint раскрывает версию приложения,
имя, и любые данные из `InfoContributor`. Это помогает атакующему определить версию.

**Исправление:**

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, prometheus   # убрать info
  endpoint:
    health:
      show-details: never             # никогда не показывать детали БД публично
      probes:
        enabled: true                 # /actuator/health/live + /actuator/health/ready для k8s
```

В `SecurityConfig` уточнить health probes:

```java
.requestMatchers("/actuator/health/live", "/actuator/health/ready").permitAll()
.requestMatchers("/actuator/health").permitAll()
.requestMatchers("/actuator/**").hasAnyRole("ARCHITECT", "VIEWER")
```

---

### П16.10 Docker: приложение запускается от root

**Файл:** `docker/Dockerfile`

**Проблема:** В Stage 2 нет инструкции `USER`. Контейнер работает от `root`, что увеличивает
ущерб при container escape.

**Исправление:**

```dockerfile
# Stage 2: Runtime
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

RUN apk add --no-cache curl git nodejs npm && \
    npm install -g @anthropic-ai/claude-code --quiet && \
    addgroup -S appgroup && adduser -S appuser -G appgroup && \
    mkdir -p /projects && chown appuser:appgroup /projects

COPY --from=builder /app/build/libs/*.jar app.jar
RUN chown appuser:appgroup app.jar

VOLUME /projects

USER appuser

EXPOSE 8081
...
```

---

## Чеклист реализации

### Спринт 1 — CRITICAL
- [ ] П16.1 IDOR: BillingController.getUsage() берёт orgId из currentUser
- [ ] П16.2 IDOR: OrganizationController.getById() проверяет ownership
- [ ] П16.3 IDOR: OrganizationController.createProject() проверяет ownership

### Спринт 2 — HIGH
- [ ] П16.4 Rate limiting на /auth/refresh
- [ ] П16.5 @Size на всех входящих DTO + server.tomcat limits
- [ ] П16.6 repoPath валидация (@Pattern + SandboxPolicy)
- [ ] П16.7 Content-Security-Policy заголовок

### Спринт 3 — MEDIUM
- [ ] П16.8 AuthRateLimitService: Caffeine с TTL и maximumSize
- [ ] П16.9 Actuator: убрать info, show-details: never
- [ ] П16.10 Docker: запуск от непривилегированного пользователя

---

## Тесты

| Тест | Класс |
|------|-------|
| `GET_usage_uses_current_user_org_id` | `BillingControllerTest` |
| `GET_organizations_id_returns_403_when_wrong_org` | `OrganizationControllerTest` |
| `POST_organizations_orgId_projects_returns_403_when_wrong_org` | `OrganizationControllerTest` |
| `POST_refresh_returns_429_when_rate_limited` | `AuthControllerTest` |
| `checkRefreshAttempt_throws_after_20_attempts` | `AuthRateLimitServiceTest` |
| `POST_login_returns_400_when_email_too_long` | `AuthControllerTest` |
| `POST_tasks_returns_400_when_title_too_long` | `TaskControllerTest` |
| `POST_organizations_orgId_projects_returns_400_when_repoPath_traversal` | `OrganizationControllerTest` |
| `GET_api_returns_csp_header` | `SecurityConfigTest` |
