package org.blacksoil.devcrew.agent.domain.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/** Domain-порт DocWriter агента. Документирует код: Javadoc, README, API-docs. */
@SystemMessage(fromResource = "prompts/doc-writer.md")
public interface DocWriterAgent {

  String execute(@UserMessage String task);
}
