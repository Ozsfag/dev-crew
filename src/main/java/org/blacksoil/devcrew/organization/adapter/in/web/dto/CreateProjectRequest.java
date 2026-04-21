package org.blacksoil.devcrew.organization.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
    @NotBlank @Size(max = 255) String name,
    @Size(max = 2048)
        @Pattern(
            regexp = "^(/[a-zA-Z0-9._-]+)+/?$|^$|^$",
            message = "repoPath должен быть абсолютным путём без '..' и специальных символов")
        String repoPath) {}
