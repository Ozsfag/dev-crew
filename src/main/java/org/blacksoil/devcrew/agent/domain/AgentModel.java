package org.blacksoil.devcrew.agent.domain;

import java.time.Instant;
import java.util.UUID;

/** Доменная модель агента. Immutable record — без Spring/JPA зависимостей. */
public record AgentModel(
    UUID id,
    AgentRole role,
    AgentStatus status,
    String systemPrompt,
    Instant createdAt,
    Instant updatedAt) {}
