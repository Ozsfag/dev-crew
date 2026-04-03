package org.blacksoil.devcrew.agent.app.config;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "devcrew.agent.rate-limit")
public class RateLimitProperties {

  /** Задержка перед повторным запуском задачи после rate-limit ошибки. */
  private Duration retryDelay = Duration.ofSeconds(60);
}
