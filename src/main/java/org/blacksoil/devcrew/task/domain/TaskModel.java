package org.blacksoil.devcrew.task.domain;

import org.blacksoil.devcrew.agent.domain.AgentRole;

import java.time.Instant;
import java.util.UUID;

/**
 * Доменная модель задачи. Immutable record.
 */
public record TaskModel(
    UUID id,
    UUID projectId,
    UUID parentTaskId,
    String title,
    String description,
    AgentRole assignedTo,
    TaskStatus status,
    String result,
    Instant createdAt,
    Instant updatedAt
) {
}
