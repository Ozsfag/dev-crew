package org.blacksoil.devcrew.organization.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateOrganizationRequest(@NotBlank String name) {}
