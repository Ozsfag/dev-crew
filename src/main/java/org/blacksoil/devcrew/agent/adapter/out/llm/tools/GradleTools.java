package org.blacksoil.devcrew.agent.adapter.out.llm.tools;

import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.agent.adapter.out.llm.process.CommandRunner;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * Инструменты для запуска Gradle-команд в целевом проекте.
 * Передаются агенту через LangChain4j AiServices.
 */
@Component
@RequiredArgsConstructor
public class GradleTools {

    private final CommandRunner commandRunner;

    @Tool("Run Gradle tests in the specified project directory and return full output")
    public String runTests(String projectPath) {
        var workDir = new File(projectPath);
        if (!workDir.exists()) {
            return "ERROR: директория не существует: " + projectPath;
        }
        var result = commandRunner.run(workDir, "./gradlew", "test");
        return formatResult(result.output(), result.exitCode());
    }

    @Tool("Build the Gradle project in the specified directory and return full output")
    public String buildProject(String projectPath) {
        var workDir = new File(projectPath);
        if (!workDir.exists()) {
            return "ERROR: директория не существует: " + projectPath;
        }
        var result = commandRunner.run(workDir, "./gradlew", "build");
        return formatResult(result.output(), result.exitCode());
    }

    private String formatResult(String output, int exitCode) {
        if (exitCode == 0) {
            return output;
        }
        return output + "\n[exit code: " + exitCode + "]";
    }
}
