package org.blacksoil.devcrew.agent.adapter.out.llm.process;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.io.File;
import lombok.RequiredArgsConstructor;

/**
 * Декоратор над CommandRunner, добавляющий метрики Micrometer. Записывает Timer
 * (devcrew.tool.execution) для каждой команды и Counter (devcrew.tool.error) при ненулевом exit
 * code.
 */
@RequiredArgsConstructor
public class MeteredCommandRunner implements CommandRunner {

  private final CommandRunner delegate;
  private final MeterRegistry meterRegistry;

  @Override
  public CommandResult run(File workDir, String... command) {
    var toolName = detectTool(command);
    var operation = detectOperation(command);
    var sample = Timer.start(meterRegistry);
    CommandResult result;
    try {
      result = delegate.run(workDir, command);
    } catch (Exception e) {
      recordError(toolName, operation);
      throw e;
    }
    sample.stop(
        Timer.builder("devcrew.tool.execution")
            .description("Время выполнения внешней команды агента")
            .tag("tool", toolName)
            .tag("operation", operation)
            .register(meterRegistry));
    if (!result.isSuccess()) {
      recordError(toolName, operation);
    }
    return result;
  }

  private void recordError(String toolName, String operation) {
    Counter.builder("devcrew.tool.error")
        .description("Количество ошибок при выполнении команд агента")
        .tag("tool", toolName)
        .tag("operation", operation)
        .register(meterRegistry)
        .increment();
  }

  static String detectTool(String... command) {
    if (command.length == 0) {
      return "unknown";
    }
    var cmd = command[0];
    if (cmd.contains("gradlew") || cmd.equals("gradle")) {
      return "gradle";
    }
    if (cmd.equals("git")) {
      return "git";
    }
    if (cmd.equals("docker")) {
      return "docker";
    }
    return "other";
  }

  static String detectOperation(String... command) {
    if (command.length < 2) {
      return "unknown";
    }
    // docker compose up → compose-up
    if (command.length >= 3 && "docker".equals(command[0]) && "compose".equals(command[1])) {
      return "compose-" + command[2];
    }
    return command[1];
  }
}
