package org.blacksoil.devcrew.agent.adapter.out.llm.process;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.blacksoil.devcrew.agent.app.config.AgentProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProcessBuilderCommandRunnerTest {

  @TempDir File tempDir;

  @Test
  void run_returns_timeout_result_when_process_exceeds_limit() {
    var properties = new AgentProperties();
    properties.setCommandTimeoutSeconds(1);
    var runner = new ProcessBuilderCommandRunner(properties);

    var result = runner.run(tempDir, "sleep", "10");

    assertThat(result.exitCode()).isEqualTo(-1);
    assertThat(result.output()).startsWith("TIMEOUT:");
  }

  @Test
  void run_returns_exit_code_and_output_on_success() {
    var properties = new AgentProperties();
    var runner = new ProcessBuilderCommandRunner(properties);

    var result = runner.run(tempDir, "echo", "hello");

    assertThat(result.exitCode()).isEqualTo(0);
    assertThat(result.output()).contains("hello");
  }

  @Test
  void run_returns_error_result_when_command_not_found() {
    var properties = new AgentProperties();
    var runner = new ProcessBuilderCommandRunner(properties);

    var result = runner.run(tempDir, "nonexistent_command_xyz_12345");

    assertThat(result.exitCode()).isEqualTo(-1);
    assertThat(result.output()).startsWith("ERROR:");
  }
}
