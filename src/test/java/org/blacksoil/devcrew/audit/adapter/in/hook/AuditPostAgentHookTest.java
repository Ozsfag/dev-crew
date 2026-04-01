package org.blacksoil.devcrew.audit.adapter.in.hook;

import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.audit.app.service.command.AuditCommandService;
import org.blacksoil.devcrew.audit.domain.AuditEventModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditPostAgentHookTest {

    @Mock
    private AuditCommandService auditCommandService;

    @InjectMocks
    private AuditPostAgentHook hook;

    @Test
    void onAgentCompleted_saves_audit_event_with_task_id() {
        var taskId = UUID.randomUUID();
        // auditCommandService.record() is void — no stubbing needed

        hook.onAgentCompleted(taskId, AgentRole.BACKEND_DEV, "result text");

        var captor = ArgumentCaptor.<AuditEventModel>captor();
        verify(auditCommandService).record(captor.capture());
        assertThat(captor.getValue().entityId()).isEqualTo(taskId);
    }

    @Test
    void onAgentCompleted_records_TASK_COMPLETED_action() {
        // auditCommandService.record() is void — no stubbing needed

        hook.onAgentCompleted(UUID.randomUUID(), AgentRole.QA, "done");

        var captor = ArgumentCaptor.<AuditEventModel>captor();
        verify(auditCommandService).record(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("TASK_COMPLETED");
    }

    @Test
    void onAgentCompleted_stores_agent_role_in_details() {
        // auditCommandService.record() is void — no stubbing needed

        hook.onAgentCompleted(UUID.randomUUID(), AgentRole.DEVOPS, "deployed");

        var captor = ArgumentCaptor.<AuditEventModel>captor();
        verify(auditCommandService).record(captor.capture());
        assertThat(captor.getValue().details()).contains("DEVOPS");
    }

    @Test
    void onAgentCompleted_uses_system_as_actor_email() {
        // auditCommandService.record() is void — no stubbing needed

        hook.onAgentCompleted(UUID.randomUUID(), AgentRole.BACKEND_DEV, "ok");

        var captor = ArgumentCaptor.<AuditEventModel>captor();
        verify(auditCommandService).record(captor.capture());
        assertThat(captor.getValue().actorEmail()).isEqualTo("system");
    }
}
