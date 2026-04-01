package org.blacksoil.devcrew.auth.domain;

import java.time.Instant;
import java.util.UUID;

public record UserModel(
    UUID id,
    String email,
    String passwordHash,
    UserRole role,
    Instant createdAt,
    Instant updatedAt
) {}
