package org.blacksoil.devcrew.agent.adapter.out.llm.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.blacksoil.devcrew.agent.app.policy.SandboxPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileToolsTest {

  @Test
  void readFile_returns_file_content(@TempDir Path tempDir) throws IOException {
    var fileTools = fileToolsWithRoot(tempDir);
    var file = tempDir.resolve("Test.java");
    Files.writeString(file, "public class Test {}");

    var content = fileTools.readFile(file.toString());

    assertThat(content).isEqualTo("public class Test {}");
  }

  @Test
  void writeFile_creates_file_with_content(@TempDir Path tempDir) throws IOException {
    var fileTools = fileToolsWithRoot(tempDir);
    var path = tempDir.resolve("NewFile.java").toString();

    fileTools.writeFile(path, "public class NewFile {}");

    assertThat(Files.readString(Path.of(path))).isEqualTo("public class NewFile {}");
  }

  @Test
  void writeFile_overwrites_existing_file(@TempDir Path tempDir) throws IOException {
    var fileTools = fileToolsWithRoot(tempDir);
    var file = tempDir.resolve("Existing.java");
    Files.writeString(file, "old content");

    fileTools.writeFile(file.toString(), "new content");

    assertThat(Files.readString(file)).isEqualTo("new content");
  }

  @Test
  void readFile_returns_error_when_file_missing(@TempDir Path tempDir) {
    var fileTools = fileToolsWithRoot(tempDir);

    var result = fileTools.readFile(tempDir.resolve("missing.java").toString());

    assertThat(result).contains("ERROR");
  }

  @Test
  void readFile_returns_error_when_path_outside_sandbox(@TempDir Path tempDir) {
    var fileTools = fileToolsWithRoot(tempDir);

    // /etc/passwd вне sandbox-корня
    var result = fileTools.readFile("/etc/passwd");

    assertThat(result).contains("ERROR");
  }

  @Test
  void listFiles_returns_files_in_directory(@TempDir Path tempDir) throws IOException {
    var fileTools = fileToolsWithRoot(tempDir);
    Files.writeString(tempDir.resolve("A.java"), "");
    Files.writeString(tempDir.resolve("B.java"), "");

    var result = fileTools.listFiles(tempDir.toString());

    assertThat(result).contains("A.java").contains("B.java");
  }

  @Test
  void listFiles_returns_error_for_path_outside_sandbox(@TempDir Path tempDir) {
    var fileTools = fileToolsWithRoot(tempDir);

    var result = fileTools.listFiles("/non/existent/dir");

    assertThat(result).contains("ERROR");
  }

  @Test
  void writeFile_returns_error_when_path_outside_sandbox(@TempDir Path tempDir) {
    var fileTools = fileToolsWithRoot(tempDir);

    var result = fileTools.writeFile("/etc/hosts", "malicious content");

    assertThat(result).contains("ERROR");
  }

  private FileTools fileToolsWithRoot(Path root) {
    return new FileTools(new SandboxPolicy(root.toString()));
  }
}
