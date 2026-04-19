package org.blacksoil.devcrew.auth.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank
        @Size(min = 8, max = 255, message = "Пароль: 8–255 символов")
        @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).+$",
            message = "Пароль должен содержать заглавные, строчные буквы и цифру")
        String password,
    String orgName // null → создать организацию с именем по умолчанию
    ) {}
