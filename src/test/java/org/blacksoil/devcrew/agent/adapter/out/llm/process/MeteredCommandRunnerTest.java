package org.blacksoil.devcrew.agent.adapter.out.llm.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.File;
import org.blacksoil.devcrew.agent.domain.shell.CommandResult;
import org.blacksoil.devcrew.agent.domain.shell.CommandRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MeteredCommandRunnerTest {

  @Mock private CommandRunner delegate;

  private SimpleMeterRegistry meterRegistry;
  private MeteredCommandRunner meteredCommandRunner;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    meteredCommandRunner = new MeteredCommandRunner(delegate, meterRegistry);
  }

  @Test
  void run_records_timer_for_gradle_test() {
    when(delegate.run(any(File.class), any(String[].class)))
        .thenReturn(new CommandResult(0, "BUILD SUCCESSFUL"));

    meteredCommandRunner.run(new File("/tmp"), "./gradlew", "test");

    var timer =
        meterRegistry
            .find("devcrew.tool.execution")
            .tag("tool", "gradle")
            .tag("operation", "test")
            .timer();
    assertThat(timer).isNotNull();
    assertThat(timer.count()).isEqualTo(1);
  }

  @Test
  void run_records_timer_for_gradle_build() {
    when(delegate.run(any(File.class), any(String[].class)))
        .thenReturn(new CommandResult(0, "BUILD SUCCESSFUL"));

    meteredCommandRunner.run(new File("/tmp"), "./gradlew", "build");

    var timer =
        meterRegistry
            .find("devcrew.tool.execution")
            .tag("tool", "gradle")
            .tag("operation", "build")
            .timer();
    assertThat(timer).isNotNull();
    assertThat(timer.count()).isEqualTo(1);
  }

  @Test
  void run_records_timer_for_git_status() {
    when(delegate.run(any(File.class), any(String[].class)))
        .thenReturn(new CommandResult(0, "On branch main"));

    meteredCommandRunner.run(new File("/tmp"), "git", "status");

    var timer =
        meterRegistry
            .find("devcrew.tool.execution")
            .tag("tool", "git")
            .tag("operation", "status")
            .timer();
    assertThat(timer).isNotNull();
    assertThat(timer.count()).isEqualTo(1);
  }

  @Test
  void run_records_timer_for_docker_build() {
    when(delegate.run(any(File.class), any(String[].class))).thenReturn(new CommandResult(0, "OK"));

    meteredCommandRunner.run(new File("/tmp"), "docker", "build", "-t", "img:1", ".");

    var timer =
        meterRegistry
            .find("devcrew.tool.execution")
            .tag("tool", "docker")
            .tag("operation", "build")
            .timer();
    assertThat(timer).isNotNull();
    assertThat(timer.count()).isEqualTo(1);
  }

  @Test
  void run_records_timer_for_docker_compose_up() {
    when(delegate.run(any(File.class), any(String[].class))).thenReturn(new CommandResult(0, "OK"));

    meteredCommandRunner.run(new File("/tmp"), "docker", "compose", "up", "-d");

    var timer =
        meterRegistry
            .find("devcrew.tool.execution")
            .tag("tool", "docker")
            .tag("operation", "compose-up")
            .timer();
    assertThat(timer).isNotNull();
    assertThat(timer.count()).isEqualTo(1);
  }

  @Test
  void run_increments_error_counter_on_nonzero_exit_code() {
    when(delegate.run(any(File.class), any(String[].class)))
        .thenReturn(new CommandResult(1, "FAILURE"));

    meteredCommandRunner.run(new File("/tmp"), "./gradlew", "test");

    var counter =
        meterRegistry
            .find("devcrew.tool.error")
            .tag("tool", "gradle")
            .tag("operation", "test")
            .counter();
    assertThat(counter).isNotNull();
    assertThat(counter.count()).isEqualTo(1.0);
  }

  @Test
  void run_does_not_increment_error_counter_on_success() {
    when(delegate.run(any(File.class), any(String[].class))).thenReturn(new CommandResult(0, "OK"));

    meteredCommandRunner.run(new File("/tmp"), "./gradlew", "test");

    var counter = meterRegistry.find("devcrew.tool.error").counter();
    assertThat(counter).isNull();
  }

  @Test
  void run_increments_error_counter_on_exception() {
    when(delegate.run(any(File.class), any(String[].class)))
        .thenThrow(new RuntimeException("process error"));

    assertThatThrownBy(() -> meteredCommandRunner.run(new File("/tmp"), "git", "push"))
        .isInstanceOf(RuntimeException.class);

    var counter =
        meterRegistry
            .find("devcrew.tool.error")
            .tag("tool", "git")
            .tag("operation", "push")
            .counter();
    assertThat(counter).isNotNull();
    assertThat(counter.count()).isEqualTo(1.0);
  }

  @Test
  void run_delegates_to_wrapped_command_runner() {
    var expected = new CommandResult(0, "output");
    when(delegate.run(any(File.class), any(String[].class))).thenReturn(expected);

    var result = meteredCommandRunner.run(new File("/tmp"), "git", "status");

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void detectTool_returns_gradle_for_gradlew() {
    assertThat(MeteredCommandRunner.detectTool("./gradlew", "test")).isEqualTo("gradle");
  }

  @Test
  void detectTool_returns_git_for_git_command() {
    assertThat(MeteredCommandRunner.detectTool("git", "status")).isEqualTo("git");
  }

  @Test
  void detectTool_returns_docker_for_docker_command() {
    assertThat(MeteredCommandRunner.detectTool("docker", "build")).isEqualTo("docker");
  }

  @Test
  void detectTool_returns_other_for_unknown_command() {
    assertThat(MeteredCommandRunner.detectTool("npm", "install")).isEqualTo("other");
  }

  @Test
  void detectTool_returns_unknown_for_empty_command() {
    assertThat(MeteredCommandRunner.detectTool()).isEqualTo("unknown");
  }

  @Test
  void detectOperation_returns_second_argument() {
    assertThat(MeteredCommandRunner.detectOperation("git", "status")).isEqualTo("status");
  }

  @Test
  void detectOperation_returns_compose_prefixed_for_docker_compose() {
    assertThat(MeteredCommandRunner.detectOperation("docker", "compose", "up", "-d"))
        .isEqualTo("compose-up");
  }

  @Test
  void detectOperation_returns_unknown_for_single_command() {
    assertThat(MeteredCommandRunner.detectOperation("git")).isEqualTo("unknown");
  }
}
