package org.blacksoil.devcrew.agent.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Конфигурация запуска Claude Code CLI (devcrew.claude-code.*). */
@Data
@ConfigurationProperties(prefix = "devcrew.claude-code")
public class ClaudeCodeProperties {

  /** Путь к исполняемому файлу claude CLI. */
  private String executable = "claude";

  /** Максимальное число итераций (turns) в одном вызове агента. */
  private int maxTurns = 20;

  /** Корневая директория sandbox — агент может работать только внутри неё. */
  private String sandboxRoot = "/projects";
}
