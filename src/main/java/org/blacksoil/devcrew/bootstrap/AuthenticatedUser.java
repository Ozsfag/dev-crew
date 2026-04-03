package org.blacksoil.devcrew.bootstrap;

import java.util.UUID;
import org.blacksoil.devcrew.auth.domain.UserRole;

/**
 * Кастомный principal — хранит userId, orgId и роль пользователя. Кладётся в SecurityContext
 * фильтром JwtAuthFilter.
 */
public record AuthenticatedUser(UUID userId, UUID orgId, UserRole role) {}
