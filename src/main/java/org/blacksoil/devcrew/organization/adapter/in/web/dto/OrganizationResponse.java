package org.blacksoil.devcrew.organization.adapter.in.web.dto;

import org.blacksoil.devcrew.organization.domain.OrgPlan;

import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        OrgPlan plan,
        Instant createdAt
) {}
