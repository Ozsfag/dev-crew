package org.blacksoil.devcrew.task.app.service.command;

import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.common.TimeProvider;
import org.blacksoil.devcrew.task.domain.TaskModel;
import org.blacksoil.devcrew.task.domain.TaskStatus;
import org.blacksoil.devcrew.task.domain.TaskStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskCommandServiceTest {

    @Mock
    private TaskStore taskStore;

    @Mock
    private TimeProvider timeProvider;

    @InjectMocks
    private TaskCommandService taskCommandService;

    @Test
    void create_saves_task_with_pending_status() {
        var now = Instant.parse("2026-01-01T10:00:00Z");
        when(timeProvider.now()).thenReturn(now);
        when(taskStore.save(any())).thenAnswer(inv -> inv.getArgument(0));

        taskCommandService.create("Write tests", "TDD for module X", AgentRole.QA, null);

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
        when(timeProvider.now()).thenReturn(Instant.now());
        when(taskStore.save(any())).thenAnswer(inv -> inv.getArgument(0));

        taskCommandService.create("Subtask", "desc", AgentRole.BACKEND_DEV, parentId);

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
        when(timeProvider.now()).thenReturn(Instant.now());
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
        when(timeProvider.now()).thenReturn(Instant.now());
        when(taskStore.save(any())).thenAnswer(inv -> inv.getArgument(0));

        taskCommandService.fail(task.id(), "Build error");

        var captor = ArgumentCaptor.<TaskModel>captor();
        verify(taskStore).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(TaskStatus.FAILED);
        assertThat(captor.getValue().result()).isEqualTo("Build error");
    }

    private TaskModel existingTask(TaskStatus status) {
        return new TaskModel(
            UUID.randomUUID(), null, "title", "desc",
            AgentRole.BACKEND_DEV, status, null,
            Instant.now(), Instant.now()
        );
    }
}
