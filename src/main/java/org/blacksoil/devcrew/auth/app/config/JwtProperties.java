package org.blacksoil.devcrew.auth.app.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "devcrew.auth.jwt")
public class JwtProperties {

  @NotBlank
  @Size(min = 32, message = "JWT secret должен быть не менее 32 символов")
  private String secret;

  private long accessTokenTtlSeconds = 3600;
  private long refreshTokenTtlSeconds = 604800;
}
