package org.blacksoil.devcrew.agent.domain.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/** Domain-порт BackendDev агента. Реализуется через LangChain4j AiServices в bootstrap-слое. */
@SystemMessage(fromResource = "prompts/backend-dev.md")
public interface BackendDevAgent {

  String execute(@UserMessage String task);
}
