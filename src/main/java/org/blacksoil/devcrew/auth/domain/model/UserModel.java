package org.blacksoil.devcrew.auth.domain.model;

import java.time.Instant;
import java.util.UUID;
import org.blacksoil.devcrew.auth.domain.UserRole;

public record UserModel(
    UUID id,
    UUID orgId,
    String email,
    String passwordHash,
    UserRole role,
    Instant createdAt,
    Instant updatedAt) {}
