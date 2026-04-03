package org.blacksoil.devcrew.task.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.blacksoil.devcrew.agent.domain.AgentRole;

public record CreateTaskRequest(
    @NotBlank String title,
    @NotBlank String description,
    @NotNull AgentRole role,
    UUID projectId // null — задача без привязки к проекту
    ) {}
