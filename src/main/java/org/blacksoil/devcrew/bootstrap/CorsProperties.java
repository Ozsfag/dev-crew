package org.blacksoil.devcrew.bootstrap;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "devcrew.cors")
public class CorsProperties {

  /** Разрешённые origins (например, https://app.example.com). Пусто = CORS отключён. */
  private List<String> allowedOrigins = List.of();

  /** Время кэширования preflight-ответа в секундах. */
  private long maxAge = 3600L;
}
