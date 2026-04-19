# П13 — Security Audit: устранение уязвимостей

**Дата аудита:** 2026-04-19  
**Аудитор:** Claude Security Review  
**Статус:** ✅⚠️ Частично выполнено (IDOR, пароль, Stripe secret, prod-логи — закрыты; rate limiting, CORS, SSL, idempotency → П14)

---

## Итоговая картина

| Критичность | Кол-во | Приоритет    |
|-------------|--------|--------------|
| CRITICAL    | 6      | Немедленно   |
| HIGH        | 3      | Эта неделя   |
| MEDIUM      | 5      | Этот месяц   |

---

## CRITICAL — немедленное устранение

### П13.1 Секреты в .env попали в git

**Файл:** `.env`  
**Проблема:**
```
TELEGRAM_BOT_TOKEN=8669495001:AAGTOjzpfhhqUiPdA0JRp2sIG0vh5LuPYN0
TELEGRAM_CHAT_ID=394503229
```
Реальный токен Telegram-бота и chat ID открыты в репозитории.

**Исправление:**
1. Немедленно пересоздать токен в @BotFather (текущий скомпрометирован)
2. Добавить `.env` в `.gitignore`
3. Удалить из git history:
   ```bash
   git rm --cached .env
   echo ".env" >> .gitignore
   git commit -m "chore: remove .env from tracking"
   # Если уже запушено — использовать git-filter-repo или BFG Cleaner
   ```
4. В production использовать только переменные окружения (Docker secrets / K8s secrets)

---

### П13.2 IDOR: GET /api/tasks/{id} без проверки org

**Файл:** `task/adapter/in/web/controller/TaskController.java`  
**Текущий код:**
```java
@GetMapping("/{id}")
public TaskDetailResponse getById(@PathVariable UUID id) {
    return mapper.toDetailResponse(taskQueryService.getById(id));
}
```
**Проблема:** Любой авторизованный пользователь может посмотреть задачу **чужой организации**, зная UUID.

**Исправление:**
```java
@GetMapping("/{id}")
public TaskDetailResponse getById(
    @PathVariable UUID id,
    @AuthenticationPrincipal AuthenticatedUser currentUser) {
  var task = taskQueryService.getById(id);
  if (!task.orgId().equals(currentUser.orgId())) {
    throw new ForbiddenException("Нет доступа к задаче");
  }
  return mapper.toDetailResponse(task);
}
```
Добавить `ForbiddenException` в `GlobalExceptionHandler` с кодом 403.  
Покрыть тестом: `GET_tasks_id_returns_403_when_wrong_org`.

---

### П13.3 IDOR: POST /api/tasks/{id}/run без проверки org

**Файл:** `task/adapter/in/web/controller/TaskController.java`  
**Текущий код:**
```java
@PostMapping("/{id}/run")
public void run(@PathVariable UUID id, @RequestParam AgentRole role) {
    taskQueryService.getById(id);
    agentOrchestrator.run(id, role);
}
```
**Проблема:** Любой пользователь может запустить агента на задаче **чужой организации**.

**Исправление:** аналогично П13.2 — добавить `@AuthenticationPrincipal` и проверку `orgId`.

---

### П13.4 Отсутствует Rate Limiting на /api/auth/**

**Файл:** `auth/adapter/in/web/controller/AuthController.java`  
**Проблема:** Нет защиты от brute-force на `/login`, `/register`, `/refresh`.

**Исправление — Resilience4j RateLimiter:**
```gradle
// build.gradle
implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.1.0'
```
```yaml
# application.yml
resilience4j.ratelimiter:
  instances:
    auth:
      limit-for-period: 10
      limit-refresh-period: 1m
      timeout-duration: 0
```
```java
@RateLimiter(name = "auth", fallbackMethod = "rateLimitFallback")
@PostMapping("/login")
public TokenResponse login(@Valid @RequestBody LoginRequest request) { ... }

public TokenResponse rateLimitFallback(LoginRequest request, RequestNotPermitted ex) {
    throw new TooManyRequestsException("Слишком много попыток входа");
}
```
Покрыть тестами: happy path + rate-limit сценарий.

---

### П13.5 Stripe webhook secret не валидируется при старте

**Файл:** `billing/app/config/BillingProperties.java`  
**Текущий код:**
```java
private String stripeWebhookSecret = "";
```
**Проблема:** Если переменная окружения не задана, проверка подписи webhook **не работает** — любой HTTP-запрос будет принят.

**Исправление:**
```java
@NotBlank(message = "devcrew.billing.stripe-webhook-secret обязателен")
private String stripeWebhookSecret;
```
Добавить `@Validated` на `BillingConfig`. Приложение не запустится без секрета.

---

### П13.6 SSL/TLS не сконфигурирован для production

**Файл:** `src/main/resources/application-prod.yml`  
**Проблема:** Нет HTTPS — токены и данные передаются в открытом виде.

**Исправление (вариант A — reverse proxy, рекомендуется):**  
Использовать Nginx или Caddy с Let's Encrypt перед приложением.
```yaml
# docker/nginx.conf
server {
    listen 443 ssl;
    ssl_certificate /etc/letsencrypt/live/domain/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/domain/privkey.pem;
    location / { proxy_pass http://app:8081; }
}
```

**Вариант B — встроенный Spring SSL:**
```yaml
# application-prod.yml
server:
  ssl:
    enabled: true
    key-store: ${SSL_KEYSTORE_PATH}
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
```

---

## HIGH — устранить в течение недели

### П13.7 Слабая валидация пароля при регистрации

**Файл:** `auth/adapter/in/web/dto/RegisterRequest.java`  
**Текущий код:** `@NotBlank String password` — принимает пароль из 1 символа.

**Исправление:**
```java
@NotBlank
@Size(min = 8, max = 255, message = "Пароль: 8–255 символов")
@Pattern(
    regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).+$",
    message = "Пароль должен содержать заглавные, строчные буквы и цифру")
String password
```
Покрыть тестом: `POST_register_returns_400_when_password_too_weak`.

---

### П13.8 Telegram allowedChatId = 0 по умолчанию

**Файл:** `notification/app/config/TelegramProperties.java`  
**Текущий код:** `private long allowedChatId = 0L;`  
**Проблема:** 0 означает «без ограничений» — любой Telegram-пользователь может управлять ботом.

**Исправление:**
```java
@Min(value = 1, message = "devcrew.notification.telegram.allowed-chat-id обязателен")
private long allowedChatId;
```
В `application.yml` явно:
```yaml
devcrew:
  notification:
    telegram:
      allowed-chat-id: ${TELEGRAM_CHAT_ID}
```

---

### П13.9 Уровни логирования не заданы явно для production

**Файл:** `src/main/resources/application-prod.yml`  
**Проблема:** Без явных уровней Spring Boot логирует INFO для всего — риск утечки внутренних деталей.

**Исправление:**
```yaml
# application-prod.yml
logging:
  level:
    root: WARN
    org.blacksoil.devcrew: INFO
    org.springframework.security: WARN
    org.hibernate.SQL: OFF
    org.hibernate.type.descriptor.sql: OFF
```

---

## MEDIUM — устранить в течение месяца

### П13.10 Idempotency на Stripe webhook

**Файл:** `billing/adapter/in/stripe/StripeWebhookAdapter.java`  
**Проблема:** Повторная доставка Stripe-события выполняет операцию дважды (двойное начисление/списание).

**Исправление:**
```java
// Сохранять обработанные Stripe Event ID в таблицу stripe_events
// и проверять перед обработкой:
if (stripeEventRepository.existsByEventId(event.getId())) {
    return ResponseEntity.ok().build(); // идемпотентный ответ
}
stripeWebhookPort.handle(event.getType(), event);
stripeEventRepository.save(new StripeEventEntity(event.getId(), Instant.now()));
```
Новая Flyway-миграция: `V{n}__add_stripe_events_table.sql`.

---

### П13.11 CORS не сконфигурирован явно

**Файл:** `bootstrap/SecurityConfig.java`  
**Проблема:** CORS не задан → Spring блокирует все кросс-доменные запросы по умолчанию. Если фронтенд на другом домене — не работает. Если кто-то добавит `@CrossOrigin("*")` на контроллере — открытая дыра.

**Исправление:**
```java
@Bean
CorsConfigurationSource corsConfigurationSource() {
    var config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(corsProperties.getAllowedOrigins()));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
}
```
В SecurityConfig: `.cors(cors -> cors.configurationSource(corsConfigurationSource()))`.

---

### П13.12 Telegram file download без валидации

**Файл:** `notification/adapter/out/telegram/TelegramApiClientImpl.java`  
**Проблема:** Скачивание файлов из Telegram по fileId без проверки типа и размера.

**Исправление:**
```java
private static final long MAX_FILE_SIZE = 20 * 1024 * 1024L; // 20 MB
private static final Set<String> ALLOWED_EXTENSIONS =
    Set.of("txt", "md", "log", "json", "xml");

// Перед обработкой файла — проверить mime type и размер
```

---

### П13.13 Production logging level не ограничен

Уже описано в П13.9.

---

## Чеклист реализации

### Спринт 1 — CRITICAL (немедленно)
- [ ] П13.1 Удалить секреты из git, пересоздать токены
- [ ] П13.2 Добавить org-проверку в GET /api/tasks/{id}
- [ ] П13.3 Добавить org-проверку в POST /api/tasks/{id}/run
- [ ] П13.4 Rate Limiting на /api/auth/**
- [ ] П13.5 Валидация stripeWebhookSecret при старте
- [ ] П13.6 SSL/TLS через nginx в docker-compose.prod.yml

### Спринт 2 — HIGH (эта неделя)
- [ ] П13.7 Валидация пароля (сложность + длина)
- [ ] П13.8 Telegram allowedChatId обязателен
- [ ] П13.9 Production log levels

### Спринт 3 — MEDIUM (этот месяц)
- [ ] П13.10 Idempotency на Stripe webhook
- [ ] П13.11 CORS явная конфигурация
- [ ] П13.12 Telegram file download валидация

---

## Тесты, которые необходимо добавить

| Тест | Класс |
|------|-------|
| `GET_tasks_id_returns_403_when_wrong_org` | `TaskControllerTest` |
| `POST_tasks_id_run_returns_403_when_wrong_org` | `TaskControllerTest` |
| `POST_login_returns_429_when_rate_limited` | `AuthControllerTest` |
| `POST_register_returns_400_when_password_too_weak` | `AuthControllerTest` |
| `POST_register_returns_400_when_password_no_uppercase` | `AuthControllerTest` |
| `POST_register_returns_400_when_password_no_digit` | `AuthControllerTest` |
| `stripeWebhookSecret_blank_causes_startup_failure` | `BillingPropertiesTest` |
| `allowedChatId_zero_causes_startup_failure` | `TelegramPropertiesTest` |
