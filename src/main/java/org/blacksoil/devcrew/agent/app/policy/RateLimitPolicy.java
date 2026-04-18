package org.blacksoil.devcrew.agent.app.policy;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.agent.app.config.RateLimitProperties;
import org.springframework.stereotype.Component;

/**
 * Определяет, является ли исключение ошибкой rate-limit от LLM, и вычисляет время повтора.
 *
 * <p>Claude Code CLI возвращает rate-limit как текст в выводе команды. Детектируем по ключевым
 * словам в сообщении исключения.
 */
@Component
@RequiredArgsConstructor
public class RateLimitPolicy {

  private final RateLimitProperties properties;

  /** Возвращает true, если исключение (или любая причина в цепочке) — rate-limit от LLM. */
  public boolean isRateLimit(Throwable ex) {
    Throwable current = ex;
    while (current != null) {
      if (matchesRateLimit(current)) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  /** Вычисляет момент времени, когда задачу можно повторить. */
  public Instant retryAt(Instant now) {
    return now.plus(properties.getRetryDelay());
  }

  private boolean matchesRateLimit(Throwable ex) {
    String message = ex.getMessage();
    if (message == null) {
      return false;
    }
    String lower = message.toLowerCase();
    return lower.contains("429")
        || lower.contains("rate limit")
        || lower.contains("rate_limit")
        || lower.contains("overloaded")
        || lower.contains("too many requests");
  }
}
