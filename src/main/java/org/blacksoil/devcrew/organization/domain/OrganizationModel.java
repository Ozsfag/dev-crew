package org.blacksoil.devcrew.organization.domain;

import java.time.Instant;
import java.util.UUID;

public record OrganizationModel(
        UUID id,
        String name,
        OrgPlan plan,
        Instant createdAt,
        Instant updatedAt
) {}
