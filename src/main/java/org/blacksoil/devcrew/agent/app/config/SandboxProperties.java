package org.blacksoil.devcrew.agent.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Конфигурация sandbox для агентов (devcrew.sandbox.*). */
@Data
@ConfigurationProperties(prefix = "devcrew.sandbox")
public class SandboxProperties {

  /** Корневая директория, за пределы которой агенты не могут читать/писать файлы. */
  private String root = "/projects";
}
