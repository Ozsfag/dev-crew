package org.blacksoil.devcrew.task.app.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.common.TimeProvider;
import org.blacksoil.devcrew.common.exception.NotFoundException;
import org.blacksoil.devcrew.task.domain.TaskModel;
import org.blacksoil.devcrew.task.domain.TaskStatus;
import org.blacksoil.devcrew.task.domain.TaskStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskCommandServiceTest {

  private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

  @Mock private TaskStore taskStore;

  @Mock private TimeProvider timeProvider;

  @InjectMocks private TaskCommandService taskCommandService;

  @Test
  void create_saves_task_with_pending_status() {
    var now = Instant.parse("2026-01-01T10:00:00Z");
    when(timeProvider.now()).thenReturn(now);
    when(taskStore.save(any())).thenAnswer(inv -> inv.getArgument(0));

    taskCommandService.create("Write tests", "TDD for module X", AgentRole.QA, null, null);

    var captor = ArgumentCaptor.<TaskModel>captor();
    verify(taskStore).save(captor.capture());

    var saved = captor.getValue();
    assertThat(saved.title()).isEqualTo("Write tests");
    assertThat(saved.assignedTo()).isEqualTo(AgentRole.QA);
    assertThat(saved.status()).isEqualTo(TaskStatus.PENDING);
    assertThat(saved.parentTaskId()).isNull();
    assertThat(saved.createdAt()).isEqualTo(now);
  }

  @Test
  void create_with_parent_id_links_subtask() {
    var parentId = UUID.randomUUID();
    when(timeProvider.now()).thenReturn(NOW);
    when(taskStore.save(any())).thenAnswer(inv -> inv.getArgument(0));

    taskCommandService.create("Subtask", "desc", AgentRole.BACKEND_DEV, null, parentId);

    var captor = ArgumentCaptor.<TaskModel>captor();
    verify(taskStore).save(captor.capture());
    assertThat(captor.getValue().parentTaskId()).isEqualTo(parentId);
  }

  @Test
  void updateStatus_changes_status_and_updatedAt() {
    var now = Instant.parse("2026-01-01T11:00:00Z");
    var task = existingTask(TaskStatus.PENDING);
    when(taskStore.findById(task.id())).thenReturn(Optional.of(task));
    when(timeProvider.now()).thenReturn(now);
    when(taskStore.save(any())).thenAnswer(inv -> inv.getArgument(0));

    taskCommandService.updateStatus(task.id(), TaskStatus.IN_PROGRESS);

    var captor = ArgumentCaptor.<TaskModel>captor();
    verify(taskStore).save(captor.capture());
    assertThat(captor.getValue().status()).isEqualTo(TaskStatus.IN_PROGRESS);
    assertThat(captor.getValue().updatedAt()).isEqualTo(now);
  }

  @Test
  void complete_sets_completed_status_and_result() {
    var task = existingTask(TaskStatus.IN_PROGRESS);
    when(taskStore.findById(task.id())).thenReturn(Optional.of(task));
    when(timeProvider.now()).thenReturn(NOW);
    when(taskStore.save(any())).thenAnswer(inv -> inv.getArgument(0));

    taskCommandService.complete(task.id(), "Done successfully");

    var captor = ArgumentCaptor.<TaskModel>captor();
    verify(taskStore).save(captor.capture());
    assertThat(captor.getValue().status()).isEqualTo(TaskStatus.COMPLETED);
    assertThat(captor.getValue().result()).isEqualTo("Done successfully");
  }

  @Test
  void fail_sets_failed_status_and_reason_as_result() {
    var task = existingTask(TaskStatus.IN_PROGRESS);
    when(taskStore.findById(task.id())).thenReturn(Optional.of(task));
    when(timeProvider.now()).thenReturn(NOW);
    when(taskStore.save(any())).thenAnswer(inv -> inv.getArgument(0));

    taskCommandService.fail(task.id(), "Build error");

    var captor = ArgumentCaptor.<TaskModel>captor();
    verify(taskStore).save(captor.capture());
    assertThat(captor.getValue().status()).isEqualTo(TaskStatus.FAILED);
    assertThat(captor.getValue().result()).isEqualTo("Build error");
  }

  @Test
  void rateLimited_sets_rate_limited_status_and_retryAt() {
    var retryAt = Instant.parse("2026-01-01T10:01:00Z");
    var task = existingTask(TaskStatus.IN_PROGRESS);
    when(taskStore.findById(task.id())).thenReturn(Optional.of(task));
    when(timeProvider.now()).thenReturn(NOW);
    when(taskStore.save(any())).thenAnswer(inv -> inv.getArgument(0));

    taskCommandService.rateLimited(task.id(), retryAt);

    var captor = ArgumentCaptor.<TaskModel>captor();
    verify(taskStore).save(captor.capture());
    assertThat(captor.getValue().status()).isEqualTo(TaskStatus.RATE_LIMITED);
    assertThat(captor.getValue().retryAt()).isEqualTo(retryAt);
  }

  @Test
  void rateLimited_preserves_existing_result() {
    var task =
        new TaskModel(
            UUID.randomUUID(),
            null,
            null,
            "title",
            "desc",
            AgentRole.BACKEND_DEV,
            TaskStatus.IN_PROGRESS,
            "partial result",
            NOW,
            NOW,
            null);
    when(taskStore.findById(task.id())).thenReturn(Optional.of(task));
    when(timeProvider.now()).thenReturn(NOW);
    when(taskStore.save(any())).thenAnswer(inv -> inv.getArgument(0));

    taskCommandService.rateLimited(task.id(), NOW.plusSeconds(60));

    var captor = ArgumentCaptor.<TaskModel>captor();
    verify(taskStore).save(captor.capture());
    assertThat(captor.getValue().result()).isEqualTo("partial result");
  }

  @Test
  void updateStatus_throws_not_found_when_task_missing() {
    var id = UUID.randomUUID();
    when(taskStore.findById(id)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> taskCommandService.updateStatus(id, TaskStatus.IN_PROGRESS))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void complete_throws_not_found_when_task_missing() {
    var id = UUID.randomUUID();
    when(taskStore.findById(id)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> taskCommandService.complete(id, "result"))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void fail_throws_not_found_when_task_missing() {
    var id = UUID.randomUUID();
    when(taskStore.findById(id)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> taskCommandService.fail(id, "reason"))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void rateLimited_throws_not_found_when_task_missing() {
    var id = UUID.randomUUID();
    when(taskStore.findById(id)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> taskCommandService.rateLimited(id, NOW.plusSeconds(60)))
        .isInstanceOf(NotFoundException.class);
  }

  private TaskModel existingTask(TaskStatus status) {
    return new TaskModel(
        UUID.randomUUID(),
        null,
        null,
        "title",
        "desc",
        AgentRole.BACKEND_DEV,
        status,
        null,
        NOW,
        NOW,
        null);
  }
}
