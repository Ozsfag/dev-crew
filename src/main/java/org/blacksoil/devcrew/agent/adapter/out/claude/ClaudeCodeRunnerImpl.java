package org.blacksoil.devcrew.agent.adapter.out.claude;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.blacksoil.devcrew.agent.app.config.ClaudeCodeProperties;
import org.blacksoil.devcrew.agent.domain.ClaudeCodeRunner;
import org.blacksoil.devcrew.agent.domain.shell.CommandRunner;
import org.springframework.stereotype.Component;

/**
 * Реализация ClaudeCodeRunner через subprocess. Создаёт временную директорию с CLAUDE.md (системный
 * промпт) и запускает claude CLI в non-interactive режиме.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClaudeCodeRunnerImpl implements ClaudeCodeRunner {

  private final CommandRunner commandRunner;
  private final ClaudeCodeProperties properties;
  private final ObjectMapper objectMapper;

  @Override
  public String run(String systemPrompt, String userPrompt) {
    // Агентный режим: инструменты разрешены, количество ходов из настроек
    return doRun(systemPrompt, userPrompt, properties.getMaxTurns(), true);
  }

  @Override
  public String run(String systemPrompt, String userPrompt, int maxTurns) {
    // Режим без инструментов: парсинг/классификация, один ход → только текстовый ответ
    return doRun(systemPrompt, userPrompt, maxTurns, false);
  }

  private String doRun(String systemPrompt, String userPrompt, int maxTurns, boolean withTools) {
    Path tempDir = null;
    try {
      tempDir = Files.createTempDirectory("devcrew-agent-");
      // Записать системный промпт как CLAUDE.md — Claude Code читает автоматически
      Files.writeString(tempDir.resolve("CLAUDE.md"), systemPrompt);
      if (withTools) {
        writeSettingsJson(tempDir);
      } else {
        writeEmptySettingsJson(tempDir);
      }

      var result =
          commandRunner.run(
              tempDir.toFile(),
              properties.getExecutable(),
              "--print",
              userPrompt,
              "--output-format",
              "json",
              "--max-turns",
              String.valueOf(maxTurns));

      if (!result.isSuccess()) {
        throw new RuntimeException("Claude CLI завершился с ошибкой: " + result.output());
      }
      return parseResult(result.output());
    } catch (IOException e) {
      throw new RuntimeException("Ошибка при запуске Claude CLI: " + e.getMessage(), e);
    } finally {
      deleteQuietly(tempDir);
    }
  }

  private String parseResult(String rawOutput) throws IOException {
    // Claude CLI может выводить предупреждения или служебные строки перед JSON.
    // Ищем первый символ '{', с которого начинается JSON-объект ответа.
    var jsonStart = rawOutput.indexOf('{');
    if (jsonStart < 0) {
      throw new IOException("Не найден JSON в выводе Claude CLI: " + rawOutput);
    }
    if (jsonStart > 0) {
      log.debug("Claude CLI вывел {} символов до JSON (предупреждения/служебный вывод)", jsonStart);
    }
    var output = objectMapper.readValue(rawOutput.substring(jsonStart), ClaudeCodeOutput.class);
    if (output.isError()) {
      throw new RuntimeException("Claude CLI вернул ошибку: " + output.result());
    }
    return output.result();
  }

  private void writeSettingsJson(Path tempDir) throws IOException {
    var claudeDir = Files.createDirectories(tempDir.resolve(".claude"));
    var sandboxRoot = properties.getSandboxRoot();
    // Разрешить инструменты без интерактивного подтверждения, ограничив sandbox-директорией
    var settings =
        """
        {"permissions":{"allow":["Read(%s/**)",\
        "Write(%s/**)",\
        "Edit(%s/**)",\
        "Bash(git *)",\
        "Bash(./gradlew *)",\
        "Bash(docker *)"]}}"""
            .formatted(sandboxRoot, sandboxRoot, sandboxRoot);
    Files.writeString(claudeDir.resolve("settings.json"), settings);
  }

  private void writeEmptySettingsJson(Path tempDir) throws IOException {
    var claudeDir = Files.createDirectories(tempDir.resolve(".claude"));
    // Запретить все инструменты — используется для задач без агентного выполнения (парсинг,
    // классификация)
    Files.writeString(claudeDir.resolve("settings.json"), "{\"permissions\":{\"allow\":[]}}");
  }

  private void deleteQuietly(Path dir) {
    if (dir == null) {
      return;
    }
    try (var walk = Files.walk(dir)) {
      walk.sorted(java.util.Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(java.io.File::delete);
    } catch (IOException e) {
      log.warn("Не удалось удалить временную директорию: {}", dir, e);
    }
  }
}
