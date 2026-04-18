package org.blacksoil.devcrew.agent.adapter.out.claude;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.ClaudeCodeRunner;
import org.blacksoil.devcrew.agent.domain.TaskParsingPort;
import org.blacksoil.devcrew.agent.domain.model.ParsedTask;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Реализует TaskParsingPort через Claude Code CLI. Загружает системный промпт из
 * prompts/task-parser.md и вызывает claude CLI для парсинга входящего сообщения пользователя в
 * структурированную задачу.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskParserClaudeAdapter implements TaskParsingPort {

  private final ClaudeCodeRunner claudeCodeRunner;
  private final ObjectMapper objectMapper;

  @Override
  public ParsedTask parse(String userMessage) {
    try {
      var systemPrompt = loadSystemPrompt();
      var json = claudeCodeRunner.run(systemPrompt, userMessage);
      return objectMapper.readValue(json, ParsedTask.class);
    } catch (Exception e) {
      log.warn("Не удалось разобрать ответ TaskParser: {}", e.getMessage());
      return new ParsedTask("", userMessage, AgentRole.BACKEND_DEV);
    }
  }

  private String loadSystemPrompt() throws IOException {
    return new ClassPathResource("prompts/task-parser.md")
        .getContentAsString(StandardCharsets.UTF_8);
  }
}
