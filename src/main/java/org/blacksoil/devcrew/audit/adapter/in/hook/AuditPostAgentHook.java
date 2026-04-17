package org.blacksoil.devcrew.audit.adapter.in.hook;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.hook.PostAgentHook;
import org.blacksoil.devcrew.audit.app.service.command.AuditCommandService;
import org.blacksoil.devcrew.audit.domain.AuditEventModel;
import org.blacksoil.devcrew.common.TimeProvider;
import org.springframework.stereotype.Component;

/**
 * Записывает событие в audit log при каждом завершении агента. Агент-модуль не знает об audit —
 * связь через PostAgentHook.
 */
@Component
@RequiredArgsConstructor
public class AuditPostAgentHook implements PostAgentHook {

  private static final int MAX_RESULT_CHARS = 1000;

  private final AuditCommandService auditCommandService;
  private final TimeProvider timeProvider;

  @Override
  public void onAgentCompleted(
      UUID taskId, UUID projectId, UUID orgId, AgentRole role, String result) {
    auditCommandService.record(
        new AuditEventModel(
            UUID.randomUUID(),
            projectId,
            null,
            "system",
            "TASK_COMPLETED",
            taskId,
            buildDetails(role, result),
            timeProvider.now()));
  }

  private String buildDetails(AgentRole role, String result) {
    var truncated =
        result != null && result.length() > MAX_RESULT_CHARS
            ? result.substring(0, MAX_RESULT_CHARS) + "..."
            : (result != null ? result : "");
    return "{\"role\":\"%s\",\"result\":\"%s\"}"
        .formatted(role.name(), truncated.replace("\"", "\\\""));
  }
}
