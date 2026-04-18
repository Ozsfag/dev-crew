package org.blacksoil.devcrew.task.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.task.domain.TaskStatus;

public record TaskDetailResponse(
    UUID id,
    UUID projectId,
    UUID parentTaskId,
    String title,
    AgentRole assignedTo,
    TaskStatus status,
    String result,
    String resultSummary,
    Instant createdAt,
    Instant updatedAt) {}
