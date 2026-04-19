package org.blacksoil.devcrew.auth.app.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.blacksoil.devcrew.common.exception.TooManyRequestsException;
import org.springframework.stereotype.Service;

@Service
public class AuthRateLimitService {

  private static final int LOGIN_LIMIT = 10;
  private static final int REGISTER_LIMIT = 5;

  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

  public void checkLoginAttempt(String email) {
    var bucket = buckets.computeIfAbsent("login:" + email, k -> loginBucket());
    if (!bucket.tryConsume(1)) {
      throw new TooManyRequestsException("Слишком много попыток входа. Повторите через 15 минут.");
    }
  }

  public void checkRegisterAttempt(String email) {
    var bucket = buckets.computeIfAbsent("register:" + email, k -> registerBucket());
    if (!bucket.tryConsume(1)) {
      throw new TooManyRequestsException("Слишком много попыток регистрации. Повторите позже.");
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
}
