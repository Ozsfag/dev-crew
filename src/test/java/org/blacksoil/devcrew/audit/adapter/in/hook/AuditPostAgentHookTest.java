package org.blacksoil.devcrew.audit.adapter.in.hook;

import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.audit.app.service.command.AuditCommandService;
import org.blacksoil.devcrew.audit.domain.AuditEventModel;
import org.blacksoil.devcrew.common.TimeProvider;
import org.blacksoil.devcrew.task.app.service.query.TaskQueryService;
import org.blacksoil.devcrew.task.domain.TaskModel;
import org.blacksoil.devcrew.task.domain.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditPostAgentHookTest {

    @Mock
    private AuditCommandService auditCommandService;

    @Mock
    private TaskQueryService taskQueryService;

    private AuditPostAgentHook hook;

    @BeforeEach
    void setUp() {
        hook = new AuditPostAgentHook(auditCommandService, taskQueryService, new TimeProvider());
    }

    @Test
    void onAgentCompleted_saves_audit_event_with_task_id() {
        var taskId = UUID.randomUUID();
        when(taskQueryService.getById(taskId)).thenReturn(taskModel(taskId));

        hook.onAgentCompleted(taskId, AgentRole.BACKEND_DEV, "result text");

        var captor = ArgumentCaptor.<AuditEventModel>captor();
        verify(auditCommandService).record(captor.capture());
        assertThat(captor.getValue().entityId()).isEqualTo(taskId);
    }

    @Test
    void onAgentCompleted_records_TASK_COMPLETED_action() {
        var taskId = UUID.randomUUID();
        when(taskQueryService.getById(taskId)).thenReturn(taskModel(taskId));

        hook.onAgentCompleted(taskId, AgentRole.QA, "done");

        var captor = ArgumentCaptor.<AuditEventModel>captor();
        verify(auditCommandService).record(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("TASK_COMPLETED");
    }

    @Test
    void onAgentCompleted_stores_agent_role_in_details() {
        var taskId = UUID.randomUUID();
        when(taskQueryService.getById(taskId)).thenReturn(taskModel(taskId));

        hook.onAgentCompleted(taskId, AgentRole.DEVOPS, "deployed");

        var captor = ArgumentCaptor.<AuditEventModel>captor();
        verify(auditCommandService).record(captor.capture());
        assertThat(captor.getValue().details()).contains("DEVOPS");
    }

    @Test
    void onAgentCompleted_uses_system_as_actor_email() {
        var taskId = UUID.randomUUID();
        when(taskQueryService.getById(taskId)).thenReturn(taskModel(taskId));

        hook.onAgentCompleted(taskId, AgentRole.BACKEND_DEV, "ok");

        var captor = ArgumentCaptor.<AuditEventModel>captor();
        verify(auditCommandService).record(captor.capture());
        assertThat(captor.getValue().actorEmail()).isEqualTo("system");
    }

    private TaskModel taskModel(UUID taskId) {
        return new TaskModel(
            taskId, UUID.randomUUID(), null, "title", "desc",
            AgentRole.BACKEND_DEV, TaskStatus.COMPLETED, null,
            Instant.now(), Instant.now()
        );
    }
}
