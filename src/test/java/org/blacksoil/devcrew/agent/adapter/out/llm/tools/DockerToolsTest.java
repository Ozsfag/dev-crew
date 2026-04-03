package org.blacksoil.devcrew.agent.adapter.out.llm.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;
import org.blacksoil.devcrew.agent.adapter.out.llm.process.CommandResult;
import org.blacksoil.devcrew.agent.adapter.out.llm.process.CommandRunner;
import org.blacksoil.devcrew.agent.app.policy.SandboxPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DockerToolsTest {

  @TempDir Path tempDir;

  @Mock private CommandRunner commandRunner;

  private DockerTools dockerTools;

  @BeforeEach
  void setUp() {
    dockerTools = new DockerTools(commandRunner, new SandboxPolicy(tempDir.toString()));
  }

  @Test
  void dockerBuild_executes_docker_build_with_tag() {
    when(commandRunner.run(any(File.class), any(String[].class)))
        .thenReturn(new CommandResult(0, "Successfully built abc123"));

    var result = dockerTools.dockerBuild(tempDir.toString(), "myapp:1.0");

    assertThat(result).contains("OK");
    var captor = ArgumentCaptor.<String[]>captor();
    verify(commandRunner).run(eq(new File(tempDir.toString())), captor.capture());
    assertThat(captor.getValue()).containsExactly("docker", "build", "-t", "myapp:1.0", ".");
  }

  @Test
  void dockerBuild_returns_error_on_failure() {
    when(commandRunner.run(any(File.class), any(String[].class)))
        .thenReturn(new CommandResult(1, "ERROR: Dockerfile not found"));

    var result = dockerTools.dockerBuild(tempDir.toString(), "myapp:1.0");

    assertThat(result).contains("ERROR");
  }

  @Test
  void dockerBuild_returns_error_for_path_outside_sandbox() {
    var result = dockerTools.dockerBuild("/etc", "myapp:1.0");
    assertThat(result).contains("ERROR");
  }

  @Test
  void dockerPush_executes_docker_push() {
    when(commandRunner.run(any(File.class), any(String[].class)))
        .thenReturn(new CommandResult(0, "latest: digest: sha256:abc pushed"));

    var result = dockerTools.dockerPush(tempDir.toString(), "registry.io/myapp:1.0");

    assertThat(result).contains("OK");
    var captor = ArgumentCaptor.<String[]>captor();
    verify(commandRunner).run(eq(new File(tempDir.toString())), captor.capture());
    assertThat(captor.getValue()).containsExactly("docker", "push", "registry.io/myapp:1.0");
  }

  @Test
  void dockerPush_returns_error_on_failure() {
    when(commandRunner.run(any(File.class), any(String[].class)))
        .thenReturn(new CommandResult(1, "unauthorized: authentication required"));

    var result = dockerTools.dockerPush(tempDir.toString(), "registry.io/myapp:1.0");

    assertThat(result).contains("ERROR");
  }

  @Test
  void dockerComposeUp_executes_docker_compose_up() {
    when(commandRunner.run(any(File.class), any(String[].class)))
        .thenReturn(new CommandResult(0, "Container app  Started"));

    var result = dockerTools.dockerComposeUp(tempDir.toString());

    assertThat(result).contains("OK");
    var captor = ArgumentCaptor.<String[]>captor();
    verify(commandRunner).run(eq(new File(tempDir.toString())), captor.capture());
    assertThat(captor.getValue())
        .containsExactly("docker", "compose", "up", "-d", "--pull", "always");
  }

  @Test
  void dockerComposePull_executes_docker_compose_pull() {
    when(commandRunner.run(any(File.class), any(String[].class)))
        .thenReturn(new CommandResult(0, "Pulling app ... done"));

    var result = dockerTools.dockerComposePull(tempDir.toString());

    assertThat(result).contains("OK");
    var captor = ArgumentCaptor.<String[]>captor();
    verify(commandRunner).run(eq(new File(tempDir.toString())), captor.capture());
    assertThat(captor.getValue()).containsExactly("docker", "compose", "pull");
  }

  @Test
  void dockerImageList_returns_image_list() {
    when(commandRunner.run(any(File.class), any(String[].class)))
        .thenReturn(new CommandResult(0, "myapp   1.0   abc123   2 hours ago   450MB"));

    var result = dockerTools.dockerImageList(tempDir.toString());

    assertThat(result).contains("myapp");
    var captor = ArgumentCaptor.<String[]>captor();
    verify(commandRunner).run(eq(new File(tempDir.toString())), captor.capture());
    assertThat(captor.getValue()).containsExactly("docker", "images");
  }
}
