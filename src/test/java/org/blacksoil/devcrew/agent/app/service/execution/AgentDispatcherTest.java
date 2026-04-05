package org.blacksoil.devcrew.agent.app.service.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.agent.BackendDevAgent;
import org.blacksoil.devcrew.agent.domain.agent.CodeReviewAgent;
import org.blacksoil.devcrew.agent.domain.agent.DevOpsAgent;
import org.blacksoil.devcrew.agent.domain.agent.DocWriterAgent;
import org.blacksoil.devcrew.agent.domain.agent.QaAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentDispatcherTest {

  @Mock private BackendDevAgent backendDevAgent;
  @Mock private QaAgent qaAgent;
  @Mock private CodeReviewAgent codeReviewAgent;
  @Mock private DevOpsAgent devOpsAgent;
  @Mock private DocWriterAgent docWriterAgent;

  private AgentDispatcher dispatcher;

  @BeforeEach
  void setUp() {
    dispatcher =
        new AgentDispatcher(backendDevAgent, qaAgent, codeReviewAgent, devOpsAgent, docWriterAgent);
  }

  @Test
  void dispatch_BACKEND_DEV_calls_backendDevAgent() {
    when(backendDevAgent.execute("task")).thenReturn("code written");

    var result = dispatcher.dispatch(AgentRole.BACKEND_DEV, "task");

    verify(backendDevAgent).execute("task");
    assertThat(result).isEqualTo("code written");
  }

  @Test
  void dispatch_QA_calls_qaAgent() {
    when(qaAgent.execute("task")).thenReturn("tests written");

    var result = dispatcher.dispatch(AgentRole.QA, "task");

    verify(qaAgent).execute("task");
    assertThat(result).isEqualTo("tests written");
  }

  @Test
  void dispatch_CODE_REVIEWER_calls_codeReviewAgent() {
    when(codeReviewAgent.execute("task")).thenReturn("APPROVE");

    var result = dispatcher.dispatch(AgentRole.CODE_REVIEWER, "task");

    verify(codeReviewAgent).execute("task");
    assertThat(result).isEqualTo("APPROVE");
  }

  @Test
  void dispatch_DEVOPS_calls_devOpsAgent() {
    when(devOpsAgent.execute("task")).thenReturn("deployed");

    var result = dispatcher.dispatch(AgentRole.DEVOPS, "task");

    verify(devOpsAgent).execute("task");
    assertThat(result).isEqualTo("deployed");
  }

  @Test
  void dispatch_DOC_WRITER_calls_docWriterAgent() {
    when(docWriterAgent.execute("task")).thenReturn("# API Documentation\n...");

    var result = dispatcher.dispatch(AgentRole.DOC_WRITER, "task");

    verify(docWriterAgent).execute("task");
    assertThat(result).isEqualTo("# API Documentation\n...");
  }
}
