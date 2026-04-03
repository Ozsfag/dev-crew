package org.blacksoil.devcrew.agent.app.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.blacksoil.devcrew.common.exception.DomainException;
import org.junit.jupiter.api.Test;
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
}
