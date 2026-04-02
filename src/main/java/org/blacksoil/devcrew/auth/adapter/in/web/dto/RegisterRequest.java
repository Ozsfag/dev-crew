package org.blacksoil.devcrew.auth.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank String password,
    String orgName   // null → создать организацию с именем по умолчанию
) {}
