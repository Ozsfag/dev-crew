package org.blacksoil.devcrew.task.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.blacksoil.devcrew.agent.domain.AgentRole;

import java.util.UUID;

public record CreateTaskRequest(
    @NotBlank String title,
    @NotBlank String description,
    @NotNull AgentRole role,
    UUID projectId   // null — задача без привязки к проекту
) {
}
