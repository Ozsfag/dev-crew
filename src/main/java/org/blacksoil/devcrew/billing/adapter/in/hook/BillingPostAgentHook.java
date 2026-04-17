package org.blacksoil.devcrew.billing.adapter.in.hook;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.hook.PostAgentHook;
import org.blacksoil.devcrew.billing.app.service.command.UsageRecordCommandService;
import org.springframework.stereotype.Component;

/**
 * После завершения агента записывает потреблённые токены и стоимость запроса. Задачи без
 * projectId/orgId пропускаются — они не привязаны к организации и биллинг не применяется.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillingPostAgentHook implements PostAgentHook {

  private final UsageRecordCommandService usageRecordCommandService;

  @Override
  public void onAgentCompleted(
      UUID taskId, UUID projectId, UUID orgId, AgentRole role, String result) {
    if (projectId == null || orgId == null) {
      log.debug("Задача {} без projectId/orgId — биллинг пропущен", taskId);
      return;
    }
    usageRecordCommandService.record(taskId, projectId, orgId, role, "", result);
    log.debug("Записано использование для задачи {}: orgId={}", taskId, orgId);
  }
}
