package org.blacksoil.devcrew.audit.domain;

import java.time.Instant;
import java.util.UUID;

public record AuditEventModel(
    UUID id,
    UUID projectId,
    String actorEmail,
    String action,
    UUID entityId,
    String details,
    Instant timestamp
) {}
