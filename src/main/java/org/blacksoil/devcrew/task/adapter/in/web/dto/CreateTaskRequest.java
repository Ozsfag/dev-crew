package org.blacksoil.devcrew.task.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.blacksoil.devcrew.agent.domain.AgentRole;

public record CreateTaskRequest(
    @NotBlank @Size(max = 500) String title,
    @NotBlank @Size(max = 20000) String description,
    @NotNull AgentRole role,
    UUID projectId // null — задача без привязки к проекту
    ) {}
