package org.blacksoil.devcrew.billing.adapter.in.hook;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.hook.PostAgentHook;
import org.blacksoil.devcrew.billing.app.service.command.UsageRecordCommandService;
import org.blacksoil.devcrew.organization.app.service.query.OrganizationQueryService;
import org.springframework.stereotype.Component;

/**
 * После завершения агента записывает потреблённые токены и стоимость запроса. Задачи без projectId
 * пропускаются — они не привязаны к организации и биллинг не применяется.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillingPostAgentHook implements PostAgentHook {

  private final OrganizationQueryService organizationQueryService;
  private final UsageRecordCommandService usageRecordCommandService;

  @Override
  public void onAgentCompleted(UUID taskId, UUID projectId, AgentRole role, String result) {
    if (projectId == null) {
      log.debug("Задача {} без projectId — биллинг пропущен", taskId);
      return;
    }
    var project = organizationQueryService.getProjectById(projectId);
    usageRecordCommandService.record(taskId, projectId, project.orgId(), role, "", result);
    log.debug("Записано использование для задачи {}: orgId={}", taskId, project.orgId());
  }
}
