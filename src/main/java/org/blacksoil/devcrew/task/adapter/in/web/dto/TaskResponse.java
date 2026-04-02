package org.blacksoil.devcrew.task.adapter.in.web.dto;

import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.task.domain.TaskStatus;

import java.time.Instant;
import java.util.UUID;

public record TaskResponse(
    UUID id,
    UUID projectId,
    UUID parentTaskId,
    String title,
    AgentRole assignedTo,
    TaskStatus status,
    String result,
    Instant createdAt,
    Instant updatedAt
) {
}
