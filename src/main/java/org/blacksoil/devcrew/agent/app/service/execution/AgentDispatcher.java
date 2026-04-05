package org.blacksoil.devcrew.agent.app.service.execution;

import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.agent.BackendDevAgent;
import org.blacksoil.devcrew.agent.domain.agent.CodeReviewAgent;
import org.blacksoil.devcrew.agent.domain.agent.DevOpsAgent;
import org.blacksoil.devcrew.agent.domain.agent.DocWriterAgent;
import org.blacksoil.devcrew.agent.domain.agent.QaAgent;
import org.springframework.stereotype.Component;

/**
 * Маршрутизирует вызов к нужному LangChain4j-агенту по роли. Выделен из AgentExecutionService,
 * чтобы не раздувать конструктор при добавлении новых агентов.
 */
@Component
@RequiredArgsConstructor
public class AgentDispatcher {

  private final BackendDevAgent backendDevAgent;
  private final QaAgent qaAgent;
  private final CodeReviewAgent codeReviewAgent;
  private final DevOpsAgent devOpsAgent;
  private final DocWriterAgent docWriterAgent;

  public String dispatch(AgentRole role, String prompt) {
    return switch (role) {
      case BACKEND_DEV -> backendDevAgent.execute(prompt);
      case QA -> qaAgent.execute(prompt);
      case CODE_REVIEWER -> codeReviewAgent.execute(prompt);
      case DEVOPS -> devOpsAgent.execute(prompt);
      case DOC_WRITER -> docWriterAgent.execute(prompt);
    };
  }
}
