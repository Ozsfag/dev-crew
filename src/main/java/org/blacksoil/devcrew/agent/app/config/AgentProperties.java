package org.blacksoil.devcrew.agent.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Конфигурация агентов через application.yml (devcrew.agent.*). */
@Data
@ConfigurationProperties(prefix = "devcrew.agent")
public class AgentProperties {

  /** Максимальное число итераций в CircularAgentPipeline. */
  private int maxIterations = 20;

  /** Таймаут выполнения одной внешней команды (git, gradle и т.д.) в секундах. */
  private int commandTimeoutSeconds = 300;

  /** Параметры CircularAgentPipeline. */
  private Pipeline pipeline = new Pipeline();

  @Data
  public static class Pipeline {

    /** Маркер, который QA-агент включает в ответ при успешной сборке. */
    private String buildSuccessfulMarker = "BUILD SUCCESSFUL";

    /** Маркер, который CodeReview-агент включает в ответ при запросе правок. */
    private String requestChangesMarker = "REQUEST_CHANGES";
  }
}
