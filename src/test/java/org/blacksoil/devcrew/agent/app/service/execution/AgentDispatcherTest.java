package org.blacksoil.devcrew.agent.app.service.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.ClaudeCodeRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentDispatcherTest {

  @Mock private ClaudeCodeRunner claudeCodeRunner;

  private AgentDispatcher dispatcher;

  @BeforeEach
  void setUp() {
    dispatcher = new AgentDispatcher(claudeCodeRunner);
  }

  @Test
  void dispatch_BACKEND_DEV_calls_runner_with_backend_dev_prompt() {
    when(claudeCodeRunner.run(contains("Java/Spring Boot developer"), eq("write code")))
        .thenReturn("code written");

    var result = dispatcher.dispatch(AgentRole.BACKEND_DEV, "write code");

    assertThat(result).isEqualTo("code written");
    verify(claudeCodeRunner).run(contains("Java/Spring Boot developer"), eq("write code"));
  }

  @Test
  void dispatch_QA_calls_runner_with_qa_prompt() {
    when(claudeCodeRunner.run(contains("QA Engineer"), eq("write tests"))).thenReturn("tests");

    var result = dispatcher.dispatch(AgentRole.QA, "write tests");

    assertThat(result).isEqualTo("tests");
    verify(claudeCodeRunner).run(contains("QA Engineer"), eq("write tests"));
  }

  @Test
  void dispatch_CODE_REVIEWER_calls_runner_with_code_review_prompt() {
    when(claudeCodeRunner.run(contains("Code Reviewer"), eq("review PR"))).thenReturn("APPROVE");

    var result = dispatcher.dispatch(AgentRole.CODE_REVIEWER, "review PR");

    assertThat(result).isEqualTo("APPROVE");
    verify(claudeCodeRunner).run(contains("Code Reviewer"), eq("review PR"));
  }

  @Test
  void dispatch_DEVOPS_calls_runner_with_devops_prompt() {
    when(claudeCodeRunner.run(contains("DevOps Engineer"), eq("deploy app")))
        .thenReturn("deployed");

    var result = dispatcher.dispatch(AgentRole.DEVOPS, "deploy app");

    assertThat(result).isEqualTo("deployed");
    verify(claudeCodeRunner).run(contains("DevOps Engineer"), eq("deploy app"));
  }

  @Test
  void dispatch_DOC_WRITER_calls_runner_with_doc_writer_prompt() {
    when(claudeCodeRunner.run(contains("Technical Writer"), eq("write docs"))).thenReturn("# Docs");

    var result = dispatcher.dispatch(AgentRole.DOC_WRITER, "write docs");

    assertThat(result).isEqualTo("# Docs");
    verify(claudeCodeRunner).run(contains("Technical Writer"), eq("write docs"));
  }

  @Test
  void dispatch_returns_result_from_runner() {
    when(claudeCodeRunner.run(anyString(), eq("task"))).thenReturn("agent result");

    var result = dispatcher.dispatch(AgentRole.BACKEND_DEV, "task");

    assertThat(result).isEqualTo("agent result");
  }

  @Test
  void dispatch_throws_UnsupportedOperationException_when_role_not_in_prompt_map() {
    // null моделирует ситуацию, когда роль добавлена в enum, но не внесена в PROMPT_FILES
    assertThatThrownBy(() -> dispatcher.dispatch(null, "task"))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining("null");
  }
}
