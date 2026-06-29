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
class ClaudeSessionRunnerImplTest {

  @Mock private CommandRunner commandRunner;

  private ClaudeSessionRunnerImpl runner;

  @BeforeEach
  void setUp() {
    runner =
        new ClaudeSessionRunnerImpl(commandRunner, new ClaudeCodeProperties(), new ObjectMapper());
  }

  @Test
  void continueSession_returns_result_and_session_id() {
    var json =
        """
        {"type":"result","result":"Готово","num_turns":2,"is_error":false,"session_id":"abc-123"}
        """;
    when(commandRunner.run(any(File.class), any(String[].class)))
        .thenReturn(new CommandResult(0, json));

    var result = runner.continueSession("/projects/vpn-app", null, "покажи структуру");

    assertThat(result.result()).isEqualTo("Готово");
    assertThat(result.sessionId()).isEqualTo("abc-123");
  }

  @Test
  void continueSession_runs_in_given_workdir() {
    when(commandRunner.run(any(File.class), any(String[].class)))
        .thenReturn(new CommandResult(0, okJson()));

    runner.continueSession("/projects/vpn-app", null, "task");

    var dirCaptor = ArgumentCaptor.<File>captor();
    verify(commandRunner).run(dirCaptor.capture(), any(String[].class));
    assertThat(dirCaptor.getValue()).isEqualTo(new File("/projects/vpn-app"));
  }

  @Test
  void continueSession_omits_resume_when_session_id_is_null() {
    when(commandRunner.run(any(File.class), any(String[].class)))
        .thenReturn(new CommandResult(0, okJson()));

    runner.continueSession("/projects/vpn-app", null, "task");

    assertThat(capturedArgs()).doesNotContain("--resume");
  }

  @Test
  void continueSession_omits_resume_when_session_id_is_blank() {
    when(commandRunner.run(any(File.class), any(String[].class)))
        .thenReturn(new CommandResult(0, okJson()));

    runner.continueSession("/projects/vpn-app", "  ", "task");

    assertThat(capturedArgs()).doesNotContain("--resume");
  }

  @Test
  void continueSession_passes_resume_with_session_id() {
    when(commandRunner.run(any(File.class), any(String[].class)))
        .thenReturn(new CommandResult(0, okJson()));

    runner.continueSession("/projects/vpn-app", "sess-9", "task");

    assertThat(capturedArgs()).containsSequence("--resume", "sess-9");
  }

  @Test
  void continueSession_passes_session_args_from_properties() {
    var props = new ClaudeCodeProperties();
    props.setSessionArgs(java.util.List.of("--dangerously-skip-permissions"));
    var customRunner = new ClaudeSessionRunnerImpl(commandRunner, props, new ObjectMapper());
    when(commandRunner.run(any(File.class), any(String[].class)))
        .thenReturn(new CommandResult(0, okJson()));

    customRunner.continueSession("/projects/vpn-app", null, "task");

    var captor = ArgumentCaptor.<String[]>captor();
    verify(commandRunner).run(any(File.class), captor.capture());
    assertThat(captor.getValue()).contains("--dangerously-skip-permissions");
  }

  @Test
  void continueSession_throws_when_is_error_true() {
    var json =
        """
        {"type":"result","result":"boom","num_turns":1,"is_error":true,"session_id":"x"}
        """;
    when(commandRunner.run(any(File.class), any(String[].class)))
        .thenReturn(new CommandResult(0, json));

    assertThatThrownBy(() -> runner.continueSession("/p", null, "t"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("boom");
  }

  @Test
  void continueSession_throws_when_command_fails() {
    when(commandRunner.run(any(File.class), any(String[].class)))
        .thenReturn(new CommandResult(1, "claude: not found"));

    assertThatThrownBy(() -> runner.continueSession("/p", null, "t"))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void continueSession_throws_when_no_json_in_output() {
    when(commandRunner.run(any(File.class), any(String[].class)))
        .thenReturn(new CommandResult(0, "no json here"));

    assertThatThrownBy(() -> runner.continueSession("/p", null, "t"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Не найден JSON");
  }

  @Test
  void continueSession_throws_when_json_is_invalid() {
    when(commandRunner.run(any(File.class), any(String[].class)))
        .thenReturn(new CommandResult(0, "{ broken json "));

    assertThatThrownBy(() -> runner.continueSession("/p", null, "t"))
        .isInstanceOf(RuntimeException.class);
  }

  private static String okJson() {
    return """
        {"type":"result","result":"ok","num_turns":1,"is_error":false,"session_id":"s1"}
        """;
  }

  private String[] capturedArgs() {
    var captor = ArgumentCaptor.<String[]>captor();
    verify(commandRunner).run(any(File.class), captor.capture());
    return captor.getValue();
  }
}
