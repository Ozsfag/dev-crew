package org.blacksoil.devcrew.agent.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.blacksoil.devcrew.agent.app.service.execution.AgentExecutionService;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.check.PreRunCheck;
import org.blacksoil.devcrew.common.exception.DomainException;
import org.blacksoil.devcrew.task.app.service.command.TaskCommandService;
import org.blacksoil.devcrew.task.app.service.query.TaskQueryService;
import org.blacksoil.devcrew.task.domain.TaskModel;
import org.blacksoil.devcrew.task.domain.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentOrchestratorImplTest {

  private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

  @Mock private TaskCommandService taskCommandService;
  @Mock private AgentExecutionService agentExecutionService;
  @Mock private TaskQueryService taskQueryService;
  @Mock private PreRunCheck preRunCheck;

  private AgentOrchestratorImpl orchestrator;

  @BeforeEach
  void setUp() {
    orchestrator =
        new AgentOrchestratorImpl(
            taskCommandService, taskQueryService, agentExecutionService, List.of(preRunCheck));
  }

  @Test
  void submit_creates_task_and_returns_its_id() {
    var expected = taskModel(UUID.randomUUID());
    when(taskCommandService.create(any(), any(), any(), any(), any())).thenReturn(expected);

    var taskId =
        orchestrator.submit("Write tests", "TDD for UserService", AgentRole.BACKEND_DEV, null);

    assertThat(taskId).isEqualTo(expected.id());
  }

  @Test
  void submit_creates_task_with_correct_role_and_no_parent() {
    var expected = taskModel(UUID.randomUUID());
    when(taskCommandService.create(any(), any(), any(), any(), any())).thenReturn(expected);

    orchestrator.submit("Write tests", "TDD for UserService", AgentRole.QA, null);

    var roleCaptor = ArgumentCaptor.<AgentRole>captor();
    verify(taskCommandService)
        .create(
            eq("Write tests"), eq("TDD for UserService"), roleCaptor.capture(), eq(null), eq(null));
    assertThat(roleCaptor.getValue()).isEqualTo(AgentRole.QA);
  }

  @Test
  void run_fetches_task_description_and_delegates_to_execution_service() {
    var taskId = UUID.randomUUID();
    var task = taskModel(taskId);
    when(taskQueryService.getById(taskId)).thenReturn(task);

    orchestrator.run(taskId, AgentRole.BACKEND_DEV);

    verify(agentExecutionService).execute(taskId, AgentRole.BACKEND_DEV, task.description());
  }

  @Test
  void run_calls_pre_run_checks_before_execution() {
    var taskId = UUID.randomUUID();
    var task = taskModel(taskId);
    when(taskQueryService.getById(taskId)).thenReturn(task);

    orchestrator.run(taskId, AgentRole.BACKEND_DEV);

    verify(preRunCheck).check(task.projectId());
    verify(agentExecutionService).execute(taskId, AgentRole.BACKEND_DEV, task.description());
  }

  @Test
  void run_aborts_when_pre_run_check_throws() {
    var taskId = UUID.randomUUID();
    var task = taskModel(taskId);
    when(taskQueryService.getById(taskId)).thenReturn(task);
    doThrow(new DomainException("Лимит задач исчерпан")).when(preRunCheck).check(any());

    assertThatThrownBy(() -> orchestrator.run(taskId, AgentRole.BACKEND_DEV))
        .isInstanceOf(DomainException.class)
        .hasMessage("Лимит задач исчерпан");
  }

  private TaskModel taskModel(UUID id) {
    return new TaskModel(
        id,
        null,
        null,
        "title",
        "description",
        AgentRole.BACKEND_DEV,
        TaskStatus.PENDING,
        null,
        NOW,
        NOW,
        null);
  }
}
