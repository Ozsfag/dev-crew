package org.blacksoil.devcrew.auth.adapter.in.web.dto;

public record RefreshResponse(
    String accessToken,
    long expiresIn
) {}
