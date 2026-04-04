package org.blacksoil.devcrew.agent.app.service.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.blacksoil.devcrew.agent.app.config.AgentProperties;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CircularAgentPipelineTest {

  private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

  @Mock private AgentDispatcher agentDispatcher;

  private CircularAgentPipeline pipeline;

  @BeforeEach
  void setUp() {
    var properties = new AgentProperties();
    properties.setMaxIterations(3);
    pipeline = new CircularAgentPipeline(agentDispatcher, properties);
  }

  @Test
  void execute_returns_review_result_when_tests_pass_and_review_approves() {
    when(agentDispatcher.dispatch(eq(AgentRole.BACKEND_DEV), contains("task")))
        .thenReturn("code v1");
    when(agentDispatcher.dispatch(eq(AgentRole.QA), contains("code v1")))
        .thenReturn("BUILD SUCCESSFUL");
    when(agentDispatcher.dispatch(eq(AgentRole.CODE_REVIEWER), contains("code v1")))
        .thenReturn("LGTM, code looks good");

    var result = pipeline.execute("task: implement feature");

    assertThat(result).isEqualTo("LGTM, code looks good");
    verify(agentDispatcher, times(1)).dispatch(eq(AgentRole.BACKEND_DEV), contains("task"));
    verify(agentDispatcher, times(1)).dispatch(eq(AgentRole.QA), contains("code v1"));
    verify(agentDispatcher, times(1)).dispatch(eq(AgentRole.CODE_REVIEWER), contains("code v1"));
  }

  @Test
  void execute_retries_backend_when_tests_fail() {
    when(agentDispatcher.dispatch(eq(AgentRole.BACKEND_DEV), contains("task")))
        .thenReturn("code v1");
    when(agentDispatcher.dispatch(eq(AgentRole.QA), contains("code v1")))
        .thenReturn("FAILED: NullPointerException");
    when(agentDispatcher.dispatch(eq(AgentRole.BACKEND_DEV), contains("FAILED")))
        .thenReturn("code v2");
    when(agentDispatcher.dispatch(eq(AgentRole.QA), contains("code v2")))
        .thenReturn("BUILD SUCCESSFUL");
    when(agentDispatcher.dispatch(eq(AgentRole.CODE_REVIEWER), contains("code v2")))
        .thenReturn("APPROVE");

    var result = pipeline.execute("task: implement feature");

    assertThat(result).isEqualTo("APPROVE");
    verify(agentDispatcher, times(2)).dispatch(eq(AgentRole.BACKEND_DEV), contains("task"));
  }

  @Test
  void execute_retries_when_code_review_requests_changes() {
    when(agentDispatcher.dispatch(eq(AgentRole.BACKEND_DEV), contains("task")))
        .thenReturn("code v1");
    when(agentDispatcher.dispatch(eq(AgentRole.QA), contains("code v1")))
        .thenReturn("BUILD SUCCESSFUL");
    when(agentDispatcher.dispatch(eq(AgentRole.CODE_REVIEWER), contains("code v1")))
        .thenReturn("REQUEST_CHANGES: refactor this method");
    when(agentDispatcher.dispatch(eq(AgentRole.BACKEND_DEV), contains("REQUEST_CHANGES")))
        .thenReturn("code v2");
    when(agentDispatcher.dispatch(eq(AgentRole.QA), contains("code v2")))
        .thenReturn("BUILD SUCCESSFUL");
    when(agentDispatcher.dispatch(eq(AgentRole.CODE_REVIEWER), contains("code v2")))
        .thenReturn("APPROVE");

    var result = pipeline.execute("task: implement feature");

    assertThat(result).isEqualTo("APPROVE");
  }

  @Test
  void execute_stops_and_returns_last_code_after_max_iterations() {
    when(agentDispatcher.dispatch(eq(AgentRole.BACKEND_DEV), contains("task")))
        .thenReturn("code attempt");
    when(agentDispatcher.dispatch(eq(AgentRole.QA), contains("code attempt")))
        .thenReturn("FAILED: tests still red");
    when(agentDispatcher.dispatch(eq(AgentRole.BACKEND_DEV), contains("FAILED")))
        .thenReturn("code attempt");

    var result = pipeline.execute("task: implement feature");

    // maxIterations=3, все 3 провалились — возвращаем последний codeResult
    assertThat(result).isEqualTo("code attempt");
    verify(agentDispatcher, times(3)).dispatch(eq(AgentRole.QA), contains("code attempt"));
  }
}
