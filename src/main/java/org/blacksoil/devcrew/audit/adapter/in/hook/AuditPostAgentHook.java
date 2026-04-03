package org.blacksoil.devcrew.audit.adapter.in.hook;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.PostAgentHook;
import org.blacksoil.devcrew.audit.app.service.command.AuditCommandService;
import org.blacksoil.devcrew.audit.domain.AuditEventModel;
import org.blacksoil.devcrew.common.TimeProvider;
import org.blacksoil.devcrew.task.app.service.query.TaskQueryService;
import org.springframework.stereotype.Component;

/**
 * Записывает событие в audit log при каждом завершении агента. Агент-модуль не знает об audit —
 * связь через PostAgentHook.
 */
@Component
@RequiredArgsConstructor
public class AuditPostAgentHook implements PostAgentHook {

  private final AuditCommandService auditCommandService;
  private final TaskQueryService taskQueryService;
  private final TimeProvider timeProvider;

  @Override
  public void onAgentCompleted(UUID taskId, AgentRole role, String result) {
    // Берём projectId из задачи для тенант-изоляции событий аудита
    var task = taskQueryService.getById(taskId);
    auditCommandService.record(
        new AuditEventModel(
            UUID.randomUUID(),
            task.projectId(),
            "system",
            "TASK_COMPLETED",
            taskId,
            "role=%s result=%s".formatted(role.name(), result),
            timeProvider.now()));
  }
}
