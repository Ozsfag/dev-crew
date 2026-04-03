package org.blacksoil.devcrew.agent.domain.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Domain-порт DevOps агента. Управляет Docker образами, deployment-конфигами, CI/CD. Реализуется
 * через LangChain4j AiServices в bootstrap-слое.
 */
@SystemMessage(fromResource = "prompts/devops.md")
public interface DevOpsAgent {

  String execute(@UserMessage String task);
}
