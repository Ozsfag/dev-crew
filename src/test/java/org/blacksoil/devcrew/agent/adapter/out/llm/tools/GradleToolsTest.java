package org.blacksoil.devcrew.agent.adapter.out.llm.tools;

import org.blacksoil.devcrew.agent.adapter.out.llm.process.CommandResult;
import org.blacksoil.devcrew.agent.adapter.out.llm.process.CommandRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradleToolsTest {

    @Mock
    private CommandRunner commandRunner;

    @InjectMocks
    private GradleTools gradleTools;

    @Test
    void runTests_executes_gradlew_test_and_returns_output(@TempDir Path tempDir) {
        var projectPath = tempDir.toString();
        when(commandRunner.run(any(File.class), any(String[].class)))
            .thenReturn(new CommandResult(0, "BUILD SUCCESSFUL"));

        var result = gradleTools.runTests(projectPath);

        assertThat(result).contains("BUILD SUCCESSFUL");
        var commandCaptor = ArgumentCaptor.<String[]>captor();
        verify(commandRunner).run(eq(new File(projectPath)), commandCaptor.capture());
        assertThat(commandCaptor.getValue()).contains("./gradlew", "test");
    }

    @Test
    void buildProject_executes_gradlew_build(@TempDir Path tempDir) {
        var projectPath = tempDir.toString();
        when(commandRunner.run(any(File.class), any(String[].class)))
            .thenReturn(new CommandResult(0, "BUILD SUCCESSFUL"));

        gradleTools.buildProject(projectPath);

        var commandCaptor = ArgumentCaptor.<String[]>captor();
        verify(commandRunner).run(eq(new File(projectPath)), commandCaptor.capture());
        assertThat(commandCaptor.getValue()).contains("./gradlew", "build");
    }

    @Test
    void runTests_includes_failure_output_on_non_zero_exit(@TempDir Path tempDir) {
        when(commandRunner.run(any(File.class), any(String[].class)))
            .thenReturn(new CommandResult(1, "FAILURE: 3 tests failed"));

        var result = gradleTools.runTests(tempDir.toString());

        assertThat(result).contains("FAILURE");
        assertThat(result).contains("exit code: 1");
    }

    @Test
    void runTests_returns_error_when_project_path_missing() {
        var result = gradleTools.runTests("/non/existent/path");

        assertThat(result).contains("ERROR");
    }
}
