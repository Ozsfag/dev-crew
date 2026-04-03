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
import org.blacksoil.devcrew.organization.domain.OrgPlan;
import org.blacksoil.devcrew.organization.domain.model.OrganizationModel;
import org.blacksoil.devcrew.organization.domain.model.ProjectModel;
import org.blacksoil.devcrew.task.app.service.query.TaskQueryService;
import org.blacksoil.devcrew.task.domain.TaskModel;
import org.blacksoil.devcrew.task.domain.TaskStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BillingPostAgentHookTest {

  @Mock private TaskQueryService taskQueryService;
  @Mock private OrganizationQueryService organizationQueryService;
  @Mock private UsageRecordCommandService usageRecordCommandService;

  @InjectMocks private BillingPostAgentHook hook;

  @Test
  void onAgentCompleted_records_usage_when_task_has_project() {
    var taskId = UUID.randomUUID();
    var projectId = UUID.randomUUID();
    var orgId = UUID.randomUUID();
    var task = taskModel(taskId, projectId);
    var project = project(projectId, orgId);
    when(taskQueryService.getById(taskId)).thenReturn(task);
    when(organizationQueryService.getProjectById(projectId)).thenReturn(project);

    hook.onAgentCompleted(taskId, AgentRole.BACKEND_DEV, "result text");

    verify(usageRecordCommandService)
        .record(
            eq(taskId),
            eq(projectId),
            eq(orgId),
            eq(AgentRole.BACKEND_DEV),
            eq(task.description()),
            eq("result text"));
  }

  @Test
  void onAgentCompleted_skips_billing_when_task_has_no_project() {
    var taskId = UUID.randomUUID();
    var task = taskModel(taskId, null);
    when(taskQueryService.getById(taskId)).thenReturn(task);

    hook.onAgentCompleted(taskId, AgentRole.QA, "result");

    verify(usageRecordCommandService, never()).record(any(), any(), any(), any(), any(), any());
  }

  private TaskModel taskModel(UUID id, UUID projectId) {
    return new TaskModel(
        id,
        projectId,
        null,
        "title",
        "Write tests",
        AgentRole.BACKEND_DEV,
        TaskStatus.COMPLETED,
        null,
        Instant.now(),
        Instant.now(),
        null);
  }

  private ProjectModel project(UUID id, UUID orgId) {
    return new ProjectModel(
        id, orgId, "my-project", "/projects/repo", Instant.now(), Instant.now());
  }

  private OrganizationModel org(UUID id) {
    return new OrganizationModel(id, "Org", OrgPlan.FREE, Instant.now(), Instant.now());
  }
}
