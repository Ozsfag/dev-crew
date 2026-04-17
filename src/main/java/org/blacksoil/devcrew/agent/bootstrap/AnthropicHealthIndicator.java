package org.blacksoil.devcrew.agent.bootstrap;

import dev.langchain4j.model.chat.ChatLanguageModel;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Health indicator для Anthropic LLM. Проверяет доступность API раз в 5 минут (кеширует результат,
 * чтобы не тратить токены на каждый health check). Активируется только при наличии
 * langchain4j.anthropic.api-key.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "langchain4j.anthropic", name = "api-key")
public class AnthropicHealthIndicator implements HealthIndicator {

  private static final Duration CHECK_INTERVAL = Duration.ofMinutes(5);

  private final ChatLanguageModel chatLanguageModel;

  private volatile Health cachedHealth = Health.unknown().build();
  private volatile Instant lastCheck = Instant.EPOCH;

  @Override
  public Health health() {
    if (Duration.between(lastCheck, Instant.now()).compareTo(CHECK_INTERVAL) > 0) {
      refreshHealth();
    }
    return cachedHealth;
  }

  private synchronized void refreshHealth() {
    // Повторная проверка внутри synchronized — избегаем двойного вызова
    if (Duration.between(lastCheck, Instant.now()).compareTo(CHECK_INTERVAL) <= 0) {
      return;
    }
    try {
      chatLanguageModel.generate("ping");
      cachedHealth = Health.up().build();
    } catch (Exception e) {
      cachedHealth = Health.down().withDetail("error", e.getMessage()).build();
    }
    lastCheck = Instant.now();
  }
}
