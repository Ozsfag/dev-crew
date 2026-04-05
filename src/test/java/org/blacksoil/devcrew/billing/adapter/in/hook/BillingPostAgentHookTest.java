package org.blacksoil.devcrew.billing.adapter.in.hook;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.billing.app.service.command.UsageRecordCommandService;
import org.blacksoil.devcrew.organization.app.service.query.OrganizationQueryService;
import org.blacksoil.devcrew.organization.domain.model.ProjectModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BillingPostAgentHookTest {

  private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

  @Mock private OrganizationQueryService organizationQueryService;
  @Mock private UsageRecordCommandService usageRecordCommandService;

  @InjectMocks private BillingPostAgentHook hook;

  @Test
  void onAgentCompleted_records_usage_when_task_has_project() {
    var taskId = UUID.randomUUID();
    var projectId = UUID.randomUUID();
    var orgId = UUID.randomUUID();
    var project = project(projectId, orgId);
    when(organizationQueryService.getProjectById(projectId)).thenReturn(project);

    hook.onAgentCompleted(taskId, projectId, AgentRole.BACKEND_DEV, "result text");

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
  void onAgentCompleted_skips_billing_when_task_has_no_project() {
    var taskId = UUID.randomUUID();

    hook.onAgentCompleted(taskId, null, AgentRole.QA, "result");

    verify(usageRecordCommandService, never()).record(any(), any(), any(), any(), any(), any());
  }

  private ProjectModel project(UUID id, UUID orgId) {
    return new ProjectModel(id, orgId, "my-project", "/projects/repo", NOW, NOW);
  }
}
