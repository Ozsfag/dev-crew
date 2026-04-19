# П14 — Security Fixes: оставшиеся уязвимости

**Статус:** 🔲 Не выполнено  
**Источник:** аудит П13 (2026-04-19) — закрытые пробелы после первого спринта

---

## П14.1 Rate Limiting на /api/auth/**

**Критичность:** CRITICAL  
**Файл:** `auth/adapter/in/web/controller/AuthController.java`

**Проблема:** Нет защиты от brute-force на `/login`, `/register`, `/refresh`. Злоумышленник может подбирать пароль без ограничений.

**Реализация — Bucket4j (in-memory, без Redis):**

```gradle
// build.gradle
implementation 'com.bucket4j:bucket4j-core:8.10.1'
```

```java
// auth/app/service/RateLimitService.java
@Service
public class RateLimitService {

  private final Cache<String, Bucket> buckets =
      Caffeine.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).build();

  public void checkLoginAttempt(String email) {
    var bucket = buckets.get(email, k -> Bucket.builder()
        .addLimit(Bandwidth.simple(10, Duration.ofMinutes(15)))
        .build());
    if (!bucket.tryConsume(1)) {
      throw new TooManyRequestsException("Слишком много попыток входа. Повторите через 15 минут.");
    }
  }
}
```

```java
// common/exception/TooManyRequestsException.java
public class TooManyRequestsException extends RuntimeException {
  public TooManyRequestsException(String message) { super(message); }
}
```

В `GlobalExceptionHandler` добавить:
```java
@ExceptionHandler(TooManyRequestsException.class)
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public ErrorResponse handleTooManyRequests(TooManyRequestsException ex) {
  return new ErrorResponse(HttpStatus.TOO_MANY_REQUESTS.value(), ex.getMessage());
}
```

В `AuthController.login()`:
```java
rateLimitService.checkLoginAttempt(request.email());
```

**Тесты:**
- `POST_login_returns_429_when_rate_limited` (мокировать `RateLimitService`)
- `checkLoginAttempt_allows_up_to_10_attempts`
- `checkLoginAttempt_throws_after_10_attempts`

---

## П14.2 CORS явная конфигурация

**Критичность:** MEDIUM  
**Файл:** `bootstrap/SecurityConfig.java`

**Проблема:** CORS не сконфигурирован — при добавлении `@CrossOrigin("*")` на любой контроллер образуется открытая дыра. Фронтенд на другом домене работать не будет.

**Реализация:**

```java
// bootstrap/CorsProperties.java
@Data
@ConfigurationProperties(prefix = "devcrew.cors")
public class CorsProperties {
  private List<String> allowedOrigins = List.of();
  private long maxAge = 3600L;
}
```

В `SecurityConfig`:
```java
@Bean
CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
  var config = new CorsConfiguration();
  config.setAllowedOrigins(corsProperties.getAllowedOrigins());
  config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
  config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
  config.setAllowCredentials(true);
  config.setMaxAge(corsProperties.getMaxAge());
  var source = new UrlBasedCorsConfigurationSource();
  source.registerCorsConfiguration("/api/**", config);
  return source;
}
```

В `.filterChain()`:
```java
.cors(cors -> cors.configurationSource(corsConfigurationSource(corsProperties)))
```

В `application.yml`:
```yaml
devcrew:
  cors:
    allowed-origins: ${DEVCREW_CORS_ALLOWED_ORIGINS:}
    max-age: 3600
```

В `application-prod.yml`:
```yaml
devcrew:
  cors:
    allowed-origins: ${DEVCREW_CORS_ALLOWED_ORIGINS}
```

**Тесты:**
- `CorsPropertiesTest` — дефолтные значения
- Интеграционный тест CORS-заголовков (опционально)

---

## П14.3 Stripe Webhook Idempotency

**Критичность:** MEDIUM  
**Файл:** `billing/adapter/in/stripe/StripeWebhookAdapter.java`

**Проблема:** При повторной доставке Stripe-события одна и та же операция (начисление/списание) выполняется дважды.

**Реализация:**

Flyway-миграция `V{n}__add_stripe_processed_events.sql`:
```sql
CREATE TABLE stripe_processed_event (
  event_id   VARCHAR(255) PRIMARY KEY,
  processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
```

```java
// billing/adapter/out/persistence/entity/StripeProcessedEventEntity.java
@Entity
@Table(name = "stripe_processed_event")
public class StripeProcessedEventEntity {
  @Id private String eventId;
  private Instant processedAt;
}

// billing/adapter/out/persistence/repository/StripeProcessedEventRepository.java
public interface StripeProcessedEventRepository
    extends JpaRepository<StripeProcessedEventEntity, String> {}
```

В `StripeWebhookAdapter`:
```java
if (stripeProcessedEventRepository.existsById(event.getId())) {
  return ResponseEntity.ok().build(); // идемпотентный ответ
}
stripeWebhookPort.handle(event.getType(), event);
stripeProcessedEventRepository.save(
    new StripeProcessedEventEntity(event.getId(), Instant.now()));
```

**Тесты:**
- `POST_webhook_returns_200_when_event_already_processed`
- `POST_webhook_processes_event_only_once`

---

## П14.4 SSL/TLS через Nginx

**Критичность:** CRITICAL (в production)  
**Файл:** `docker/docker-compose.prod.yml`

**Проблема:** Трафик между клиентом и сервером не зашифрован.

**Реализация — Nginx reverse proxy с Let's Encrypt:**

`docker/nginx/nginx.conf`:
```nginx
server {
    listen 80;
    server_name ${DOMAIN};
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name ${DOMAIN};

    ssl_certificate     /etc/letsencrypt/live/${DOMAIN}/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/${DOMAIN}/privkey.pem;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;

    location / {
        proxy_pass http://app:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

В `docker-compose.prod.yml` добавить сервис `nginx`:
```yaml
nginx:
  image: nginx:alpine
  ports:
    - "80:80"
    - "443:443"
  volumes:
    - ./nginx/nginx.conf:/etc/nginx/conf.d/default.conf:ro
    - /etc/letsencrypt:/etc/letsencrypt:ro
  depends_on:
    - app
```

В `application-prod.yml` добавить:
```yaml
server:
  forward-headers-strategy: native  # доверять X-Forwarded-Proto от Nginx
```

**Чеклист:**
- [ ] Создать `docker/nginx/nginx.conf`
- [ ] Добавить nginx-сервис в `docker-compose.prod.yml`
- [ ] Настроить Let's Encrypt (certbot) на сервере
- [ ] Добавить `forward-headers-strategy` в `application-prod.yml`

---

## П14.5 Telegram allowedChatId обязателен при botEnabled=true

**Критичность:** HIGH  
**Файл:** `notification/app/config/TelegramProperties.java`

**Проблема:** При `botEnabled=true` и `allowedChatId=0` бот принимает команды от любого Telegram-пользователя.

**Реализация — валидация при старте:**

```java
// notification/app/config/TelegramBotValidator.java
@Component
@RequiredArgsConstructor
public class TelegramBotValidator implements InitializingBean {

  private final TelegramProperties properties;

  @Override
  public void afterPropertiesSet() {
    if (properties.isBotEnabled() && properties.getAllowedChatId() == 0L) {
      throw new IllegalStateException(
          "devcrew.notification.telegram.allowed-chat-id обязателен при botEnabled=true");
    }
  }
}
```

**Тест:**
- `TelegramBotValidatorTest.afterPropertiesSet_throws_when_botEnabled_and_chatId_zero`
- `TelegramBotValidatorTest.afterPropertiesSet_ok_when_botDisabled`
- `TelegramBotValidatorTest.afterPropertiesSet_ok_when_chatId_set`

---

## Чеклист реализации

### Спринт 1 — CRITICAL
- [ ] П14.1 Rate Limiting (Bucket4j) на /api/auth/**
- [ ] П14.4 SSL/TLS через Nginx

### Спринт 2 — HIGH / MEDIUM
- [ ] П14.5 Telegram allowedChatId валидация при старте
- [ ] П14.2 CORS явная конфигурация
- [ ] П14.3 Stripe webhook idempotency

---

## Тесты

| Тест | Класс |
|------|-------|
| `POST_login_returns_429_when_rate_limited` | `AuthControllerTest` |
| `checkLoginAttempt_allows_up_to_10_attempts` | `RateLimitServiceTest` |
| `checkLoginAttempt_throws_after_10_attempts` | `RateLimitServiceTest` |
| `afterPropertiesSet_throws_when_botEnabled_and_chatId_zero` | `TelegramBotValidatorTest` |
| `afterPropertiesSet_ok_when_botDisabled` | `TelegramBotValidatorTest` |
| `afterPropertiesSet_ok_when_chatId_set` | `TelegramBotValidatorTest` |
| `POST_webhook_returns_200_when_event_already_processed` | `StripeWebhookAdapterTest` |
