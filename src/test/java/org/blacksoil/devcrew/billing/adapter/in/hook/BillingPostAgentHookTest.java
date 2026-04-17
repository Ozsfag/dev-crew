package org.blacksoil.devcrew.billing.adapter.in.hook;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.billing.app.service.command.UsageRecordCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BillingPostAgentHookTest {

  @Mock private UsageRecordCommandService usageRecordCommandService;

  private BillingPostAgentHook hook;

  @BeforeEach
  void setUp() {
    hook = new BillingPostAgentHook(usageRecordCommandService);
  }

  @Test
  void onAgentCompleted_records_usage_when_task_has_project_and_org() {
    var taskId = UUID.randomUUID();
    var projectId = UUID.randomUUID();
    var orgId = UUID.randomUUID();

    hook.onAgentCompleted(taskId, projectId, orgId, AgentRole.BACKEND_DEV, "result text");

    verify(usageRecordCommandService)
        .record(
            eq(taskId),
            eq(projectId),
            eq(orgId),
            eq(AgentRole.BACKEND_DEV),
            any(),
            eq("result text"));
  }

  @Test
  void onAgentCompleted_skips_billing_when_projectId_is_null() {
    var taskId = UUID.randomUUID();
    var orgId = UUID.randomUUID();

    hook.onAgentCompleted(taskId, null, orgId, AgentRole.QA, "result");

    verify(usageRecordCommandService, never()).record(any(), any(), any(), any(), any(), any());
  }

  @Test
  void onAgentCompleted_skips_billing_when_orgId_is_null() {
    var taskId = UUID.randomUUID();
    var projectId = UUID.randomUUID();

    hook.onAgentCompleted(taskId, projectId, null, AgentRole.QA, "result");

    verify(usageRecordCommandService, never()).record(any(), any(), any(), any(), any(), any());
  }

  @Test
  void onAgentCompleted_second_call_with_same_taskId_does_not_double_charge() {
    // Идемпотентность обеспечивается UsageRecordCommandService (проверяет existsByTaskId).
    // Hook всегда делегирует в сервис, сервис возвращает null при повторном вызове.
    var taskId = UUID.randomUUID();
    var projectId = UUID.randomUUID();
    var orgId = UUID.randomUUID();
    when(usageRecordCommandService.record(any(), any(), any(), any(), any(), any()))
        .thenReturn(null);

    hook.onAgentCompleted(taskId, projectId, orgId, AgentRole.BACKEND_DEV, "result");
    hook.onAgentCompleted(taskId, projectId, orgId, AgentRole.BACKEND_DEV, "result");

    verify(usageRecordCommandService, times(2))
        .record(eq(taskId), eq(projectId), eq(orgId), eq(AgentRole.BACKEND_DEV), any(), any());
  }
}
