package org.blacksoil.devcrew.agent.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.AgentStatus;

public record AgentResponse(
    UUID id, AgentRole role, AgentStatus status, Instant createdAt, Instant updatedAt) {}
