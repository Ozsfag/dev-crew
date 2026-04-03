package org.blacksoil.devcrew.organization.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;
import org.blacksoil.devcrew.organization.domain.OrgPlan;

public record OrganizationResponse(UUID id, String name, OrgPlan plan, Instant createdAt) {}
