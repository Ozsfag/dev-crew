package org.blacksoil.devcrew.agent.adapter.out.claude;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import org.blacksoil.devcrew.agent.app.config.ClaudeCodeProperties;
import org.blacksoil.devcrew.agent.domain.shell.CommandResult;
import org.blacksoil.devcrew.agent.domain.shell.CommandRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClaudeCodeRunnerImplTest {

  @Mock private CommandRunner commandRunner;

  private ClaudeCodeRunnerImpl runner;

  @BeforeEach
  void setUp() {
    var properties = new ClaudeCodeProperties();
    runner = new ClaudeCodeRunnerImpl(commandRunner, properties, new ObjectMapper());
  }

  @Test
  void run_returns_result_on_success() {
    var json =
        """
        {"type":"result","result":"Task completed","num_turns":3,"is_error":false}
        """;
    when(commandRunner.run(any(File.class), any(String[].class)))
        .thenReturn(new CommandResult(0, json));

    var result = runner.run("You are a developer.", "Fix the bug");

    assertThat(result).isEqualTo("Task completed");
  }

  @Test
  void run_passes_correct_cli_arguments() {
    var json =
        """
        {"type":"result","result":"done","num_turns":1,"is_error":false}
        """;
    when(commandRunner.run(any(File.class), any(String[].class)))
        .thenReturn(new CommandResult(0, json));

    runner.run("system", "user task");

    var captor = ArgumentCaptor.<String[]>captor();
    verify(commandRunner).run(any(File.class), captor.capture());
    var args = captor.getValue();
    assertThat(args[0]).isEqualTo("claude");
    assertThat(args[1]).isEqualTo("--print");
    assertThat(args[2]).isEqualTo("user task");
    assertThat(args).contains("--output-format", "json");
    assertThat(args).contains("--max-turns", "20");
  }

  @Test
  void run_throws_when_is_error_true() {
    var json =
        """
        {"type":"result","result":"Something went wrong","num_turns":1,"is_error":true}
        """;
    when(commandRunner.run(any(File.class), any(String[].class)))
        .thenReturn(new CommandResult(0, json));

    assertThatThrownBy(() -> runner.run("system", "task"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Something went wrong");
  }

  @Test
  void run_throws_when_command_fails_with_nonzero_exit() {
    when(commandRunner.run(any(File.class), any(String[].class)))
        .thenReturn(new CommandResult(1, "claude: command not found"));

    assertThatThrownBy(() -> runner.run("system", "task")).isInstanceOf(RuntimeException.class);
  }

  @Test
  void run_throws_when_json_is_invalid() {
    when(commandRunner.run(any(File.class), any(String[].class)))
        .thenReturn(new CommandResult(0, "not valid json"));

    assertThatThrownBy(() -> runner.run("system", "task")).isInstanceOf(RuntimeException.class);
  }

  @Test
  void run_uses_executable_from_properties() {
    var properties = new ClaudeCodeProperties();
    properties.setExecutable("/usr/local/bin/claude");
    var customRunner = new ClaudeCodeRunnerImpl(commandRunner, properties, new ObjectMapper());

    var json =
        """
        {"type":"result","result":"ok","num_turns":1,"is_error":false}
        """;
    when(commandRunner.run(any(File.class), any(String[].class)))
        .thenReturn(new CommandResult(0, json));

    customRunner.run("system", "task");

    var captor = ArgumentCaptor.<String[]>captor();
    verify(commandRunner).run(any(File.class), captor.capture());
    assertThat(captor.getValue()[0]).isEqualTo("/usr/local/bin/claude");
  }

  @Test
  void run_uses_max_turns_from_properties() {
    var properties = new ClaudeCodeProperties();
    properties.setMaxTurns(5);
    var customRunner = new ClaudeCodeRunnerImpl(commandRunner, properties, new ObjectMapper());

    var json =
        """
        {"type":"result","result":"ok","num_turns":1,"is_error":false}
        """;
    when(commandRunner.run(any(File.class), any(String[].class)))
        .thenReturn(new CommandResult(0, json));

    customRunner.run("system", "task");

    var captor = ArgumentCaptor.<String[]>captor();
    verify(commandRunner).run(any(File.class), captor.capture());
    assertThat(captor.getValue()).contains("5");
  }
}
