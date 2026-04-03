package org.blacksoil.devcrew.billing.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.blacksoil.devcrew.agent.domain.AgentRole;

/** Запись о потреблении токенов за выполнение одной задачи агентом. */
public record UsageRecordModel(
    UUID id,
    UUID taskId,
    UUID projectId,
    UUID orgId,
    AgentRole agentRole,
    int promptTokens,
    int completionTokens,
    BigDecimal costUsd,
    Instant recordedAt) {}
