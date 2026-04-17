package org.blacksoil.devcrew.audit.adapter.in.hook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.audit.app.service.command.AuditCommandService;
import org.blacksoil.devcrew.audit.domain.AuditEventModel;
import org.blacksoil.devcrew.common.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditPostAgentHookTest {

  @Mock private AuditCommandService auditCommandService;

  private AuditPostAgentHook hook;

  @BeforeEach
  void setUp() {
    hook = new AuditPostAgentHook(auditCommandService, new TimeProvider());
  }

  @Test
  void onAgentCompleted_saves_audit_event_with_task_id() {
    var taskId = UUID.randomUUID();
    var projectId = UUID.randomUUID();

    hook.onAgentCompleted(
        taskId, projectId, UUID.randomUUID(), AgentRole.BACKEND_DEV, "result text");

    var captor = ArgumentCaptor.<AuditEventModel>captor();
    verify(auditCommandService).record(captor.capture());
    assertThat(captor.getValue().entityId()).isEqualTo(taskId);
  }

  @Test
  void onAgentCompleted_records_TASK_COMPLETED_action() {
    var taskId = UUID.randomUUID();
    var projectId = UUID.randomUUID();

    hook.onAgentCompleted(taskId, projectId, UUID.randomUUID(), AgentRole.QA, "done");

    var captor = ArgumentCaptor.<AuditEventModel>captor();
    verify(auditCommandService).record(captor.capture());
    assertThat(captor.getValue().action()).isEqualTo("TASK_COMPLETED");
  }

  @Test
  void onAgentCompleted_stores_agent_role_in_details() {
    var taskId = UUID.randomUUID();
    var projectId = UUID.randomUUID();

    hook.onAgentCompleted(taskId, projectId, UUID.randomUUID(), AgentRole.DEVOPS, "deployed");

    var captor = ArgumentCaptor.<AuditEventModel>captor();
    verify(auditCommandService).record(captor.capture());
    assertThat(captor.getValue().details()).contains("DEVOPS");
  }

  @Test
  void onAgentCompleted_uses_system_as_actor_email() {
    var taskId = UUID.randomUUID();
    var projectId = UUID.randomUUID();

    hook.onAgentCompleted(taskId, projectId, UUID.randomUUID(), AgentRole.BACKEND_DEV, "ok");

    var captor = ArgumentCaptor.<AuditEventModel>captor();
    verify(auditCommandService).record(captor.capture());
    assertThat(captor.getValue().actorEmail()).isEqualTo("system");
  }

  @Test
  void onAgentCompleted_truncates_result_exceeding_1000_chars() {
    var taskId = UUID.randomUUID();
    var projectId = UUID.randomUUID();
    var longResult = "x".repeat(2000);

    hook.onAgentCompleted(taskId, projectId, UUID.randomUUID(), AgentRole.BACKEND_DEV, longResult);

    var captor = ArgumentCaptor.<AuditEventModel>captor();
    verify(auditCommandService).record(captor.capture());
    assertThat(captor.getValue().details()).contains("...");
    assertThat(captor.getValue().details()).doesNotContain("x".repeat(1001));
  }

  @Test
  void onAgentCompleted_details_uses_json_format() {
    var taskId = UUID.randomUUID();
    var projectId = UUID.randomUUID();

    hook.onAgentCompleted(taskId, projectId, UUID.randomUUID(), AgentRole.QA, "tests passed");

    var captor = ArgumentCaptor.<AuditEventModel>captor();
    verify(auditCommandService).record(captor.capture());
    assertThat(captor.getValue().details()).startsWith("{");
    assertThat(captor.getValue().details()).contains("\"role\"");
    assertThat(captor.getValue().details()).contains("\"result\"");
  }
}
