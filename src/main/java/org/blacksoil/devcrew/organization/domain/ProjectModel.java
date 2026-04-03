package org.blacksoil.devcrew.organization.domain;

import java.time.Instant;
import java.util.UUID;

public record ProjectModel(
    UUID id, UUID orgId, String name, String repoPath, Instant createdAt, Instant updatedAt) {}
