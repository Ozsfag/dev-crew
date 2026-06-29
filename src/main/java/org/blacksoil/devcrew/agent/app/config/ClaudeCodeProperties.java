package org.blacksoil.devcrew.agent.app.config;

import java.util.ArrayList;
import java.util.List;
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

  /**
   * Доп. аргументы claude для интерактивного сеанса Telegram (режим разрешений и пр.). По умолчанию
   * автоматически принимаются правки файлов. Для полной автономии (запуск bash/тестов без
   * подтверждения) задать ["--dangerously-skip-permissions"] — осознанный выбор, т.к. агент
   * получает право исполнять произвольные команды в репозитории.
   */
  private List<String> sessionArgs = new ArrayList<>(List.of("--permission-mode", "acceptEdits"));
}
