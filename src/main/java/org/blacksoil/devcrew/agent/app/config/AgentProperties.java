package org.blacksoil.devcrew.agent.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Конфигурация агентов через application.yml (devcrew.agent.*). */
@Data
@ConfigurationProperties(prefix = "devcrew.agent")
public class AgentProperties {

  /** Модель Claude, используемая агентами по умолчанию. */
  private String model = "claude-sonnet-4-6";

  /** Максимальное число токенов в одном ответе агента. */
  private int maxTokens = 8096;

  /** Максимальное число итераций tool-use в одном вызове. */
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
