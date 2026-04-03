package org.blacksoil.devcrew.auth.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "devcrew.auth.jwt")
public class JwtProperties {

  private String secret = "change-me-min-32-chars-long-secret!!";
  private long accessTokenTtlSeconds = 3600;
  private long refreshTokenTtlSeconds = 604800;
}
