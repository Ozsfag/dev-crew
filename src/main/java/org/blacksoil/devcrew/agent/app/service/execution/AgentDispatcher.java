package org.blacksoil.devcrew.agent.app.service.execution;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.ClaudeCodeRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Маршрутизирует вызов к Claude Code CLI по роли агента. Загружает системный промпт из classpath
 * (prompts/*.md) и передаёт его вместе с задачей в ClaudeCodeRunner.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentDispatcher {

  private final ClaudeCodeRunner claudeCodeRunner;

  private static final Map<AgentRole, String> PROMPT_FILES =
      Map.of(
          AgentRole.BACKEND_DEV, "prompts/backend-dev.md",
          AgentRole.QA, "prompts/qa.md",
          AgentRole.CODE_REVIEWER, "prompts/code-review.md",
          AgentRole.DEVOPS, "prompts/devops.md",
          AgentRole.DOC_WRITER, "prompts/doc-writer.md");

  /** Запускает Claude Code CLI с системным промптом нужной роли и возвращает результат. */
  public String dispatch(AgentRole role, String prompt) {
    var systemPrompt = loadPrompt(PROMPT_FILES.get(role));
    log.debug("Диспетчеризация задачи агенту: role={}", role);
    return claudeCodeRunner.run(systemPrompt, prompt);
  }

  private String loadPrompt(String resourcePath) {
    try {
      var resource = new ClassPathResource(resourcePath);
      return resource.getContentAsString(StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException("Не удалось загрузить системный промпт: " + resourcePath, e);
    }
  }
}
