package org.blacksoil.devcrew.auth.app.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.blacksoil.devcrew.common.exception.TooManyRequestsException;
import org.springframework.stereotype.Service;

@Service
public class AuthRateLimitService {

  private static final int LOGIN_LIMIT = 10;
  private static final int REGISTER_LIMIT = 5;
  private static final int REFRESH_LIMIT = 20;

  // TTL 1 час после последнего обращения; жёсткий лимит 100K ключей — защита от OOM
  private final Cache<String, Bucket> buckets =
      Caffeine.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).maximumSize(100_000).build();

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

  public void checkRefreshAttempt(String token) {
    // Используем первые 16 символов как ключ — не логируем полный токен
    var prefix = token.substring(0, Math.min(16, token.length()));
    var bucket = buckets.get("refresh:" + prefix, k -> refreshBucket());
    if (!bucket.tryConsume(1)) {
      throw new TooManyRequestsException("Слишком много попыток обновления токена.");
    }
  }

  private static Bucket loginBucket() {
    return Bucket.builder()
        .addLimit(
            Bandwidth.builder()
                .capacity(LOGIN_LIMIT)
                .refillGreedy(LOGIN_LIMIT, Duration.ofMinutes(15))
                .build())
        .build();
  }

  private static Bucket registerBucket() {
    return Bucket.builder()
        .addLimit(
            Bandwidth.builder()
                .capacity(REGISTER_LIMIT)
                .refillGreedy(REGISTER_LIMIT, Duration.ofHours(1))
                .build())
        .build();
  }

  private static Bucket refreshBucket() {
    return Bucket.builder()
        .addLimit(
            Bandwidth.builder()
                .capacity(REFRESH_LIMIT)
                .refillGreedy(REFRESH_LIMIT, Duration.ofMinutes(5))
                .build())
        .build();
  }
}
