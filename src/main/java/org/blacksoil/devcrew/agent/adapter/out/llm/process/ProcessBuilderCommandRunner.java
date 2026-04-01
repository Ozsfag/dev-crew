package org.blacksoil.devcrew.agent.adapter.out.llm.process;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Реализация CommandRunner через ProcessBuilder.
 */
@Component
public class ProcessBuilderCommandRunner implements CommandRunner {

    @Override
    public CommandResult run(File workDir, String... command) {
        try {
            var process = new ProcessBuilder(Arrays.asList(command))
                .directory(workDir)
                .redirectErrorStream(true)
                .start();

            var output = new String(process.getInputStream().readAllBytes());
            var exitCode = process.waitFor();
            return new CommandResult(exitCode, output);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return new CommandResult(-1, "ERROR: " + e.getMessage());
        }
    }
}
