package org.blacksoil.devcrew.auth.app.service;

import org.blacksoil.devcrew.auth.app.config.JwtProperties;
import org.blacksoil.devcrew.auth.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        var props = new JwtProperties();
        // 32+ символа для HMAC-SHA256
        props.setSecret("test-secret-min-32-chars-long-ok!");
        props.setAccessTokenTtlSeconds(3600);
        props.setRefreshTokenTtlSeconds(604800);
        jwtService = new JwtService(props);
    }

    @Test
    void generateAccessToken_returns_non_blank_token() {
        var userId = UUID.randomUUID();
        var token = jwtService.generateAccessToken(userId, "user@test.com", UserRole.ARCHITECT);
        assertThat(token).isNotBlank();
    }

    @Test
    void generateRefreshToken_returns_non_blank_token() {
        var token = jwtService.generateRefreshToken(UUID.randomUUID());
        assertThat(token).isNotBlank();
    }

    @Test
    void validateAccessToken_does_not_throw_for_valid_token() {
        var userId = UUID.randomUUID();
        var token = jwtService.generateAccessToken(userId, "user@test.com", UserRole.VIEWER);
        assertThatNoException().isThrownBy(() -> jwtService.validateAccessToken(token));
    }

    @Test
    void validateAccessToken_throws_for_invalid_token() {
        assertThatThrownBy(() -> jwtService.validateAccessToken("not.a.token"))
            .isInstanceOf(Exception.class);
    }

    @Test
    void extractUserId_returns_correct_id() {
        var userId = UUID.randomUUID();
        var token = jwtService.generateAccessToken(userId, "user@test.com", UserRole.ARCHITECT);
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    void extractRole_returns_correct_role() {
        var token = jwtService.generateAccessToken(UUID.randomUUID(), "user@test.com", UserRole.VIEWER);
        assertThat(jwtService.extractRole(token)).isEqualTo(UserRole.VIEWER);
    }

    @Test
    void hashToken_same_input_returns_same_hash() {
        var raw = "some-random-token-value";
        assertThat(jwtService.hashToken(raw)).isEqualTo(jwtService.hashToken(raw));
    }

    @Test
    void hashToken_different_input_returns_different_hash() {
        assertThat(jwtService.hashToken("token-a")).isNotEqualTo(jwtService.hashToken("token-b"));
    }

    @Test
    void getAccessTokenTtlSeconds_returns_configured_value() {
        assertThat(jwtService.getAccessTokenTtlSeconds()).isEqualTo(3600);
    }

    @Test
    void getRefreshTokenTtlSeconds_returns_configured_value() {
        assertThat(jwtService.getRefreshTokenTtlSeconds()).isEqualTo(604800);
    }
}
