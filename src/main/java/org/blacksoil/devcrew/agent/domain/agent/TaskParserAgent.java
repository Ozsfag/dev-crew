package org.blacksoil.devcrew.agent.domain.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * LangChain4j агент-парсер: преобразует свободный текст пользователя в структурированный
 * JSON-запрос задачи.
 */
@SystemMessage(fromResource = "prompts/task-parser.md")
public interface TaskParserAgent {

  @UserMessage("Parse this message into a task: {{message}}")
  String parse(String message);
}
