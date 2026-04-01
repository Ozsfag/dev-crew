package org.blacksoil.devcrew.auth.adapter.in.web.dto;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    long expiresIn
) {}
