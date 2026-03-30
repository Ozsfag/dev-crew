package org.blacksoil.devcrew.agent.adapter.in.web.dto;

import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.AgentStatus;

import java.time.Instant;
import java.util.UUID;

public record AgentResponse(
    UUID id,
    AgentRole role,
    AgentStatus status,
    Instant createdAt,
    Instant updatedAt
) {
}
