package org.blacksoil.devcrew.agent.domain.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Domain-порт QA агента. Пишет тесты, запускает их, репортит покрытие. Реализуется через
 * LangChain4j AiServices в bootstrap-слое.
 */
@SystemMessage(fromResource = "prompts/qa.md")
public interface QaAgent {

  String execute(@UserMessage String task);
}
