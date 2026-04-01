package org.blacksoil.devcrew.audit.adapter.in.hook;

import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.PostAgentHook;
import org.blacksoil.devcrew.audit.app.service.command.AuditCommandService;
import org.blacksoil.devcrew.audit.domain.AuditEventModel;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Записывает событие в audit log при каждом завершении агента.
 * Агент-модуль не знает об audit — связь через PostAgentHook.
 */
@Component
@RequiredArgsConstructor
public class AuditPostAgentHook implements PostAgentHook {

    private final AuditCommandService auditCommandService;

    @Override
    public void onAgentCompleted(UUID taskId, AgentRole role, String result) {
        auditCommandService.record(new AuditEventModel(
            UUID.randomUUID(),
            "system",
            "TASK_COMPLETED",
            taskId,
            "role=%s result=%s".formatted(role.name(), result),
            Instant.now()
        ));
    }
}
