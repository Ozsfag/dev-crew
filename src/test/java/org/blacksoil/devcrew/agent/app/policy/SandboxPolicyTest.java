package org.blacksoil.devcrew.agent.app.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.blacksoil.devcrew.common.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SandboxPolicyTest {

  private final SandboxPolicy policy = new SandboxPolicy("/projects");

  @Test
  void valid_path_inside_root_passes() {
    assertThatCode(() -> policy.validatePath("/projects/my-app/src/Main.java"))
        .doesNotThrowAnyException();
  }

  @Test
  void valid_directory_inside_root_passes() {
    assertThatCode(() -> policy.validatePath("/projects/my-app/")).doesNotThrowAnyException();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/etc/passwd",
        "/bin/bash",
        "/home/user/.ssh/id_rsa",
        "relative/path/file.java",
        ""
      })
  void path_outside_root_is_rejected(String path) {
    assertThatThrownBy(() -> policy.validatePath(path))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("доступ запрещён");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/projects/../etc/passwd",
        "/projects/app/../../etc/passwd",
        "/projects/./../../secret"
      })
  void path_traversal_is_rejected(String path) {
    assertThatThrownBy(() -> policy.validatePath(path))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("доступ запрещён");
  }

  @Test
  void null_path_is_rejected() {
    assertThatThrownBy(() -> policy.validatePath(null)).isInstanceOf(DomainException.class);
  }

  @Test
  void root_itself_passes() {
    assertThatCode(() -> policy.validatePath("/projects")).doesNotThrowAnyException();
  }

  @Test
  void validatePath_throws_when_symlink_points_outside_sandbox(
      @TempDir Path sandbox, @TempDir Path outside) throws Exception {
    var sandboxPolicy = new SandboxPolicy(sandbox.toString());
    Files.createFile(outside.resolve("secret.txt"));
    var symlink = sandbox.resolve("evil");
    Files.createSymbolicLink(symlink, outside.resolve("secret.txt"));

    assertThatThrownBy(() -> sandboxPolicy.validatePath(symlink.toString()))
        .isInstanceOf(DomainException.class);
  }

  @Test
  void validatePath_allows_valid_nonexistent_path_inside_sandbox(@TempDir Path sandbox) {
    var sandboxPolicy = new SandboxPolicy(sandbox.toString());
    var nonExistent = sandbox.resolve("NewFile.java").toString();

    assertThatNoException().isThrownBy(() -> sandboxPolicy.validatePath(nonExistent));
  }
}
