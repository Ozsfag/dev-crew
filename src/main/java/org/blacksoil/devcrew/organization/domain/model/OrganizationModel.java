package org.blacksoil.devcrew.organization.domain.model;

import java.time.Instant;
import java.util.UUID;
import org.blacksoil.devcrew.organization.domain.OrgPlan;

public record OrganizationModel(
    UUID id,
    String name,
    OrgPlan plan,
    String stripeCustomerId,
    Instant createdAt,
    Instant updatedAt) {}
