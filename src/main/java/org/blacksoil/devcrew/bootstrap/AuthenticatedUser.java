package org.blacksoil.devcrew.bootstrap;

import org.blacksoil.devcrew.auth.domain.UserRole;

import java.util.UUID;

/**
 * Кастомный principal — хранит userId, orgId и роль пользователя.
 * Кладётся в SecurityContext фильтром JwtAuthFilter.
 */
public record AuthenticatedUser(
        UUID userId,
        UUID orgId,
        UserRole role
) {}
