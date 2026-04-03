package org.blacksoil.devcrew.agent.domain.model;

import java.time.Instant;
import java.util.UUID;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.AgentStatus;

/** Доменная модель агента. Immutable record — без Spring/JPA зависимостей. */
public record AgentModel(
    UUID id,
    AgentRole role,
    AgentStatus status,
    String systemPrompt,
    Instant createdAt,
    Instant updatedAt) {}
