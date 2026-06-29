package org.blacksoil.devcrew.agent.adapter.out.claude;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.blacksoil.devcrew.agent.app.config.ClaudeCodeProperties;
import org.blacksoil.devcrew.agent.domain.ClaudeSessionRunner;
import org.blacksoil.devcrew.agent.domain.model.SessionResult;
import org.blacksoil.devcrew.agent.domain.shell.CommandRunner;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * Реализация ClaudeSessionRunner через subprocess. Запускает claude CLI прямо в папке репозитория
 * (агент использует его собственный CLAUDE.md) и продолжает диалог через --resume.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClaudeSessionRunnerImpl implements ClaudeSessionRunner {

  private final CommandRunner commandRunner;
  private final ClaudeCodeProperties properties;
  private final ObjectMapper objectMapper;

  @Override
  public SessionResult continueSession(
      String workDir, @Nullable String sessionId, String userMessage) {
    var command = new ArrayList<String>();
    command.add(properties.getExecutable());
    command.add("--print");
    command.add(userMessage);
    command.add("--output-format");
    command.add("json");
    // Продолжаем существующий сеанс — контекст диалога сохраняется, токены кэшируются
    if (sessionId != null && !sessionId.isBlank()) {
      command.add("--resume");
      command.add(sessionId);
    }
    command.addAll(properties.getSessionArgs());

    log.debug("Запуск сеанса claude: workDir={}, resume={}", workDir, sessionId != null);
    var result = commandRunner.run(new File(workDir), command.toArray(new String[0]));
    if (!result.isSuccess()) {
      throw new RuntimeException("Claude CLI завершился с ошибкой: " + result.output());
    }
    return parse(result.output());
  }

  private SessionResult parse(String rawOutput) {
    // Claude CLI может вывести предупреждения перед JSON — ищем первый '{'
    var jsonStart = rawOutput.indexOf('{');
    if (jsonStart < 0) {
      throw new RuntimeException("Не найден JSON в выводе Claude CLI: " + rawOutput);
    }
    try {
      var output = objectMapper.readValue(rawOutput.substring(jsonStart), ClaudeCodeOutput.class);
      if (output.isError()) {
        throw new RuntimeException("Claude CLI вернул ошибку: " + output.result());
      }
      return new SessionResult(output.result(), output.sessionId());
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Ошибка парсинга вывода Claude CLI: " + e.getMessage(), e);
    }
  }
}
