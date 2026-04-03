package org.blacksoil.devcrew.agent.bootstrap;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.blacksoil.devcrew.agent.domain.AgentOrchestrator;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.common.TimeProvider;
import org.blacksoil.devcrew.task.app.service.query.TaskQueryService;
import org.blacksoil.devcrew.task.domain.TaskModel;
import org.blacksoil.devcrew.task.domain.TaskStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RateLimitRetrySchedulerTest {

  @Mock private TaskQueryService taskQueryService;

  @Mock private AgentOrchestrator agentOrchestrator;

  @Mock private TimeProvider timeProvider;

  @InjectMocks private RateLimitRetryScheduler scheduler;

  @Test
  void retryRateLimitedTasks_calls_run_for_each_ready_task() {
    var now = Instant.parse("2026-01-01T10:00:00Z");
    var task1 = rateLimitedTask(AgentRole.BACKEND_DEV);
    var task2 = rateLimitedTask(AgentRole.QA);
    when(timeProvider.now()).thenReturn(now);
    when(taskQueryService.getRateLimitedReadyToRetry(now)).thenReturn(List.of(task1, task2));

    scheduler.retryRateLimitedTasks();

    verify(agentOrchestrator).run(task1.id(), AgentRole.BACKEND_DEV);
    verify(agentOrchestrator).run(task2.id(), AgentRole.QA);
  }

  @Test
  void retryRateLimitedTasks_does_nothing_when_no_tasks_ready() {
    var now = Instant.parse("2026-01-01T10:00:00Z");
    when(timeProvider.now()).thenReturn(now);
    when(taskQueryService.getRateLimitedReadyToRetry(now)).thenReturn(List.of());

    scheduler.retryRateLimitedTasks();

    verify(agentOrchestrator, never()).run(any(), any());
  }

  private TaskModel rateLimitedTask(AgentRole role) {
    return new TaskModel(
        UUID.randomUUID(),
        null,
        null,
        "title",
        "description",
        role,
        TaskStatus.RATE_LIMITED,
        null,
        Instant.now(),
        Instant.now(),
        Instant.now().minusSeconds(1));
  }

  private static <T> T any() {
    return org.mockito.ArgumentMatchers.any();
  }
}
