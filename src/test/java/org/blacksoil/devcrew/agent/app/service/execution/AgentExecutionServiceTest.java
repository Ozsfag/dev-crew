package org.blacksoil.devcrew.agent.app.service.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.blacksoil.devcrew.agent.app.config.RateLimitProperties;
import org.blacksoil.devcrew.agent.app.policy.RateLimitPolicy;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.hook.PostAgentHook;
import org.blacksoil.devcrew.common.TimeProvider;
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
class AgentExecutionServiceTest {

  @Mock private AgentDispatcher agentDispatcher;

  @Mock private TaskQueryService taskQueryService;

  @Mock private TaskCommandService taskCommandService;

  @Mock private PostAgentHook postAgentHook;

  @Mock private TimeProvider timeProvider;

  private AgentExecutionService agentExecutionService;
  private SimpleMeterRegistry meterRegistry;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    var rateLimitPolicy = new RateLimitPolicy(new RateLimitProperties());
    agentExecutionService =
        new AgentExecutionService(
            agentDispatcher,
            taskQueryService,
            taskCommandService,
            List.of(postAgentHook),
            meterRegistry,
            rateLimitPolicy,
            timeProvider);
  }

  @Test
  void execute_sets_task_in_progress_then_delegates_to_dispatcher() {
    var taskId = UUID.randomUUID();
    var task = taskModel(taskId, TaskStatus.PENDING);
    when(taskQueryService.getById(taskId)).thenReturn(task);
    when(agentDispatcher.dispatch(any(), any())).thenReturn("tests written");
    when(taskCommandService.updateStatus(any(), any())).thenReturn(task);
    when(taskCommandService.complete(any(), any())).thenReturn(task);

    agentExecutionService.execute(taskId, AgentRole.BACKEND_DEV, "write unit tests");

    verify(taskCommandService).updateStatus(taskId, TaskStatus.IN_PROGRESS);
    verify(agentDispatcher).dispatch(AgentRole.BACKEND_DEV, "write unit tests");
  }

  @Test
  void execute_completes_task_with_dispatcher_result() {
    var taskId = UUID.randomUUID();
    var task = taskModel(taskId, TaskStatus.IN_PROGRESS);
    when(taskQueryService.getById(taskId)).thenReturn(task);
    when(agentDispatcher.dispatch(any(), any())).thenReturn("done: created FooTest.java");
    when(taskCommandService.updateStatus(any(), any())).thenReturn(task);
    when(taskCommandService.complete(any(), any())).thenReturn(task);

    agentExecutionService.execute(taskId, AgentRole.BACKEND_DEV, "write unit tests");

    verify(taskCommandService).complete(taskId, "done: created FooTest.java");
  }

  @Test
  void execute_fails_task_when_dispatcher_throws() {
    var taskId = UUID.randomUUID();
    var task = taskModel(taskId, TaskStatus.IN_PROGRESS);
    when(taskQueryService.getById(taskId)).thenReturn(task);
    when(taskCommandService.updateStatus(any(), any())).thenReturn(task);
    when(agentDispatcher.dispatch(any(), any())).thenThrow(new RuntimeException("LLM error"));
    when(taskCommandService.fail(any(), any())).thenReturn(task);

    agentExecutionService.execute(taskId, AgentRole.BACKEND_DEV, "write unit tests");

    verify(taskCommandService).fail(eq(taskId), any());
  }

  @Test
  void execute_calls_post_hooks_after_completion() {
    var taskId = UUID.randomUUID();
    var task = taskModel(taskId, TaskStatus.PENDING);
    when(taskQueryService.getById(taskId)).thenReturn(task);
    when(agentDispatcher.dispatch(any(), any())).thenReturn("result");
    when(taskCommandService.updateStatus(any(), any())).thenReturn(task);
    when(taskCommandService.complete(any(), any())).thenReturn(task);

    agentExecutionService.execute(taskId, AgentRole.BACKEND_DEV, "task prompt");

    var hookCaptor = ArgumentCaptor.<String>captor();
    verify(postAgentHook)
        .onAgentCompleted(eq(taskId), any(), eq(AgentRole.BACKEND_DEV), hookCaptor.capture());
    assertThat(hookCaptor.getValue()).isEqualTo("result");
  }

  @Test
  void execute_dispatches_DEVOPS_role_to_dispatcher() {
    var taskId = UUID.randomUUID();
    var task = taskModel(taskId, TaskStatus.PENDING);
    when(taskQueryService.getById(taskId)).thenReturn(task);
    when(agentDispatcher.dispatch(eq(AgentRole.DEVOPS), any())).thenReturn("OK: image built");
    when(taskCommandService.updateStatus(any(), any())).thenReturn(task);
    when(taskCommandService.complete(any(), any())).thenReturn(task);

    agentExecutionService.execute(taskId, AgentRole.DEVOPS, "build and push myapp:1.0");

    verify(agentDispatcher).dispatch(AgentRole.DEVOPS, "build and push myapp:1.0");
  }

  @Test
  void execute_dispatches_CODE_REVIEWER_role_to_dispatcher() {
    var taskId = UUID.randomUUID();
    var task = taskModel(taskId, TaskStatus.PENDING);
    when(taskQueryService.getById(taskId)).thenReturn(task);
    when(agentDispatcher.dispatch(eq(AgentRole.CODE_REVIEWER), any()))
        .thenReturn("## Review\n✅ APPROVE");
    when(taskCommandService.updateStatus(any(), any())).thenReturn(task);
    when(taskCommandService.complete(any(), any())).thenReturn(task);

    agentExecutionService.execute(taskId, AgentRole.CODE_REVIEWER, "review PR #42");

    verify(agentDispatcher).dispatch(AgentRole.CODE_REVIEWER, "review PR #42");
  }

  @Test
  void execute_dispatches_QA_role_to_dispatcher() {
    var taskId = UUID.randomUUID();
    var task = taskModel(taskId, TaskStatus.PENDING);
    when(taskQueryService.getById(taskId)).thenReturn(task);
    when(agentDispatcher.dispatch(eq(AgentRole.QA), any())).thenReturn("tests written: 15 passing");
    when(taskCommandService.updateStatus(any(), any())).thenReturn(task);
    when(taskCommandService.complete(any(), any())).thenReturn(task);

    agentExecutionService.execute(taskId, AgentRole.QA, "write tests for UserService");

    verify(agentDispatcher).dispatch(AgentRole.QA, "write tests for UserService");
  }

  @Test
  void execute_dispatches_DOC_WRITER_role_to_dispatcher() {
    var taskId = UUID.randomUUID();
    var task = taskModel(taskId, TaskStatus.PENDING);
    when(taskQueryService.getById(taskId)).thenReturn(task);
    when(agentDispatcher.dispatch(eq(AgentRole.DOC_WRITER), any()))
        .thenReturn("# API Documentation\n...");
    when(taskCommandService.updateStatus(any(), any())).thenReturn(task);
    when(taskCommandService.complete(any(), any())).thenReturn(task);

    agentExecutionService.execute(taskId, AgentRole.DOC_WRITER, "document PaymentService");

    verify(agentDispatcher).dispatch(AgentRole.DOC_WRITER, "document PaymentService");
  }

  @Test
  void execute_increments_COMPLETED_counter_on_success() {
    var taskId = UUID.randomUUID();
    var task = taskModel(taskId, TaskStatus.PENDING);
    when(taskQueryService.getById(taskId)).thenReturn(task);
    when(agentDispatcher.dispatch(any(), any())).thenReturn("done");
    when(taskCommandService.updateStatus(any(), any())).thenReturn(task);
    when(taskCommandService.complete(any(), any())).thenReturn(task);

    agentExecutionService.execute(taskId, AgentRole.BACKEND_DEV, "prompt");

    var counter =
        meterRegistry
            .find("devcrew.task.total")
            .tag("status", "COMPLETED")
            .tag("role", AgentRole.BACKEND_DEV.name())
            .counter();
    assertThat(counter).isNotNull();
    assertThat(counter.count()).isEqualTo(1.0);
  }

  @Test
  void execute_increments_FAILED_counter_on_exception() {
    var taskId = UUID.randomUUID();
    var task = taskModel(taskId, TaskStatus.PENDING);
    when(taskQueryService.getById(taskId)).thenReturn(task);
    when(taskCommandService.updateStatus(any(), any())).thenReturn(task);
    when(agentDispatcher.dispatch(any(), any())).thenThrow(new RuntimeException("LLM error"));
    when(taskCommandService.fail(any(), any())).thenReturn(task);

    agentExecutionService.execute(taskId, AgentRole.BACKEND_DEV, "prompt");

    var counter =
        meterRegistry
            .find("devcrew.task.total")
            .tag("status", "FAILED")
            .tag("role", AgentRole.BACKEND_DEV.name())
            .counter();
    assertThat(counter).isNotNull();
    assertThat(counter.count()).isEqualTo(1.0);
  }

  @Test
  void execute_records_agent_duration_timer_on_success() {
    var taskId = UUID.randomUUID();
    var task = taskModel(taskId, TaskStatus.PENDING);
    when(taskQueryService.getById(taskId)).thenReturn(task);
    when(agentDispatcher.dispatch(any(), any())).thenReturn("done");
    when(taskCommandService.updateStatus(any(), any())).thenReturn(task);
    when(taskCommandService.complete(any(), any())).thenReturn(task);

    agentExecutionService.execute(taskId, AgentRole.BACKEND_DEV, "prompt");

    var timer =
        meterRegistry
            .find("devcrew.agent.duration")
            .tag("role", AgentRole.BACKEND_DEV.name())
            .timer();
    assertThat(timer).isNotNull();
    assertThat(timer.count()).isEqualTo(1);
  }

  @Test
  void execute_marks_task_rate_limited_when_llm_returns_429() {
    var taskId = UUID.randomUUID();
    var task = taskModel(taskId, TaskStatus.PENDING);
    var now = Instant.parse("2026-01-01T10:00:00Z");
    when(taskQueryService.getById(taskId)).thenReturn(task);
    when(taskCommandService.updateStatus(any(), any())).thenReturn(task);
    when(agentDispatcher.dispatch(any(), any()))
        .thenThrow(new RuntimeException("HTTP 429 Too Many Requests"));
    when(timeProvider.now()).thenReturn(now);
    when(taskCommandService.rateLimited(any(), any())).thenReturn(task);

    agentExecutionService.execute(taskId, AgentRole.BACKEND_DEV, "prompt");

    var captor = ArgumentCaptor.<java.time.Instant>captor();
    verify(taskCommandService).rateLimited(eq(taskId), captor.capture());
    assertThat(captor.getValue()).isAfter(now);
  }

  @Test
  void execute_does_not_call_fail_when_rate_limited() {
    var taskId = UUID.randomUUID();
    var task = taskModel(taskId, TaskStatus.PENDING);
    when(taskQueryService.getById(taskId)).thenReturn(task);
    when(taskCommandService.updateStatus(any(), any())).thenReturn(task);
    when(agentDispatcher.dispatch(any(), any()))
        .thenThrow(new RuntimeException("rate limit exceeded"));
    when(timeProvider.now()).thenReturn(Instant.parse("2026-01-01T10:00:00Z"));
    when(taskCommandService.rateLimited(any(), any())).thenReturn(task);

    agentExecutionService.execute(taskId, AgentRole.BACKEND_DEV, "prompt");

    org.mockito.Mockito.verify(taskCommandService, org.mockito.Mockito.never()).fail(any(), any());
  }

  @Test
  void execute_increments_RATE_LIMITED_counter_on_rate_limit_error() {
    var taskId = UUID.randomUUID();
    var task = taskModel(taskId, TaskStatus.PENDING);
    when(taskQueryService.getById(taskId)).thenReturn(task);
    when(taskCommandService.updateStatus(any(), any())).thenReturn(task);
    when(agentDispatcher.dispatch(any(), any())).thenThrow(new RuntimeException("HTTP 429"));
    when(timeProvider.now()).thenReturn(Instant.parse("2026-01-01T10:00:00Z"));
    when(taskCommandService.rateLimited(any(), any())).thenReturn(task);

    agentExecutionService.execute(taskId, AgentRole.BACKEND_DEV, "prompt");

    var counter =
        meterRegistry
            .find("devcrew.task.total")
            .tag("status", "RATE_LIMITED")
            .tag("role", AgentRole.BACKEND_DEV.name())
            .counter();
    assertThat(counter).isNotNull();
    assertThat(counter.count()).isEqualTo(1.0);
  }

  private TaskModel taskModel(UUID id, TaskStatus status) {
    return new TaskModel(
        id,
        null,
        null,
        "title",
        "description",
        AgentRole.BACKEND_DEV,
        status,
        null,
        Instant.parse("2026-01-01T10:00:00Z"),
        Instant.parse("2026-01-01T10:00:00Z"),
        null);
  }
}
