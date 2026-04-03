package org.blacksoil.devcrew.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

public record RefreshTokenModel(
    UUID id,
    UUID userId,
    String tokenHash,
    Instant expiresAt,
    boolean revoked,
    Instant createdAt) {}
