package org.blacksoil.devcrew.agent.adapter.out.llm.process;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.agent.app.config.AgentProperties;
import org.springframework.stereotype.Component;

/** Реализация CommandRunner через ProcessBuilder. Применяет таймаут из AgentProperties. */
@Component
@RequiredArgsConstructor
public class ProcessBuilderCommandRunner implements CommandRunner {

  private final AgentProperties agentProperties;

  @Override
  public CommandResult run(File workDir, String... command) {
    Process process;
    try {
      process =
          new ProcessBuilder(Arrays.asList(command))
              .directory(workDir)
              .redirectErrorStream(true)
              .start();
    } catch (IOException e) {
      Thread.currentThread().interrupt();
      return new CommandResult(-1, "ERROR: " + e.getMessage());
    }

    // Читаем вывод в отдельном потоке чтобы не заблокировать pipe-буфер
    var outputHolder = new StringBuilder();
    var readerThread =
        new Thread(
            () -> {
              try {
                outputHolder.append(new String(process.getInputStream().readAllBytes()));
              } catch (IOException e) {
                outputHolder.append("ERROR reading output: ").append(e.getMessage());
              }
            });
    readerThread.start();

    try {
      var timeoutSeconds = agentProperties.getCommandTimeoutSeconds();
      var finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
      if (!finished) {
        process.destroyForcibly();
        readerThread.interrupt();
        return new CommandResult(
            -1, "TIMEOUT: команда превысила лимит %d секунд".formatted(timeoutSeconds));
      }
      readerThread.join(5_000);
      return new CommandResult(process.exitValue(), outputHolder.toString());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      process.destroyForcibly();
      return new CommandResult(-1, "ERROR: поток прерван: " + e.getMessage());
    }
  }
}
