package org.blacksoil.devcrew.organization.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
    UUID id, UUID orgId, String name, String repoPath, Instant createdAt) {}
