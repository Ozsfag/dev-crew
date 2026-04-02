package org.blacksoil.devcrew.audit.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditResponse(
    UUID id,
    UUID projectId,
    String actorEmail,
    String action,
    UUID entityId,
    String details,
    Instant timestamp
) {}
