package org.blacksoil.devcrew.task.domain;

import java.time.Instant;
import java.util.UUID;
import org.blacksoil.devcrew.agent.domain.AgentRole;

/** Доменная модель задачи. Immutable record. */
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
    Instant updatedAt) {}
