package org.blacksoil.devcrew.auth.app.service;

import org.blacksoil.devcrew.auth.domain.*;
import org.blacksoil.devcrew.common.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserStore userStore;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userStore, refreshTokenStore, jwtService, new BCryptPasswordEncoder());
    }

    // --- register ---

    @Test
    void register_first_user_gets_ARCHITECT_role() {
        when(userStore.existsAny()).thenReturn(false);
        when(userStore.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userStore.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(refreshTokenStore.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateAccessToken(any(), anyString(), any())).thenReturn("access");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh");
        when(jwtService.hashToken(anyString())).thenReturn("hash");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(3600L);
        when(jwtService.getRefreshTokenTtlSeconds()).thenReturn(604800L);

        authService.register("admin@test.com", "password");

        var captor = ArgumentCaptor.<UserModel>captor();
        verify(userStore).save(captor.capture());
        assertThat(captor.getValue().role()).isEqualTo(UserRole.ARCHITECT);
    }

    @Test
    void register_second_user_gets_VIEWER_role() {
        when(userStore.existsAny()).thenReturn(true);
        when(userStore.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userStore.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(refreshTokenStore.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateAccessToken(any(), anyString(), any())).thenReturn("access");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh");
        when(jwtService.hashToken(anyString())).thenReturn("hash");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(3600L);
        when(jwtService.getRefreshTokenTtlSeconds()).thenReturn(604800L);

        authService.register("viewer@test.com", "password");

        var captor = ArgumentCaptor.<UserModel>captor();
        verify(userStore).save(captor.capture());
        assertThat(captor.getValue().role()).isEqualTo(UserRole.VIEWER);
    }

    @Test
    void register_duplicate_email_throws_ConflictException() {
        when(userStore.findByEmail("dup@test.com")).thenReturn(
            Optional.of(userModel("dup@test.com", UserRole.VIEWER))
        );

        assertThatThrownBy(() -> authService.register("dup@test.com", "password"))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    void register_returns_login_result_with_tokens() {
        when(userStore.existsAny()).thenReturn(false);
        when(userStore.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userStore.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(refreshTokenStore.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateAccessToken(any(), anyString(), any())).thenReturn("myAccess");
        when(jwtService.generateRefreshToken(any())).thenReturn("myRefresh");
        when(jwtService.hashToken("myRefresh")).thenReturn("myHash");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(3600L);
        when(jwtService.getRefreshTokenTtlSeconds()).thenReturn(604800L);

        var result = authService.register("admin@test.com", "pass");

        assertThat(result.accessToken()).isEqualTo("myAccess");
        assertThat(result.refreshToken()).isEqualTo("myRefresh");
        assertThat(result.expiresIn()).isEqualTo(3600L);
    }

    // --- login ---

    @Test
    void login_valid_credentials_returns_tokens() {
        var encoder = new BCryptPasswordEncoder();
        var user = new UserModel(UUID.randomUUID(), "user@test.com",
            encoder.encode("secret"), UserRole.ARCHITECT, Instant.now(), Instant.now());
        when(userStore.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(refreshTokenStore.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateAccessToken(any(), anyString(), any())).thenReturn("at");
        when(jwtService.generateRefreshToken(any())).thenReturn("rt");
        when(jwtService.hashToken("rt")).thenReturn("rth");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(3600L);
        when(jwtService.getRefreshTokenTtlSeconds()).thenReturn(604800L);

        var result = authService.login("user@test.com", "secret");

        assertThat(result.accessToken()).isEqualTo("at");
        assertThat(result.refreshToken()).isEqualTo("rt");
    }

    @Test
    void login_wrong_password_throws_AuthException() {
        var encoder = new BCryptPasswordEncoder();
        var user = new UserModel(UUID.randomUUID(), "user@test.com",
            encoder.encode("correct"), UserRole.VIEWER, Instant.now(), Instant.now());
        when(userStore.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login("user@test.com", "wrong"))
            .isInstanceOf(AuthException.class);
    }

    @Test
    void login_unknown_email_throws_AuthException() {
        when(userStore.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("nobody@test.com", "pass"))
            .isInstanceOf(AuthException.class);
    }

    // --- refresh ---

    @Test
    void refresh_valid_token_returns_new_access_token() {
        var userId = UUID.randomUUID();
        var user = userModel("u@test.com", UserRole.VIEWER);
        var tokenModel = new RefreshTokenModel(UUID.randomUUID(), userId,
            "hash", Instant.now().plusSeconds(3600), false, Instant.now());

        when(jwtService.hashToken("raw")).thenReturn("hash");
        when(refreshTokenStore.findByTokenHash("hash")).thenReturn(Optional.of(tokenModel));
        when(userStore.findById(tokenModel.userId())).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(any(), anyString(), any())).thenReturn("newAt");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(3600L);

        var result = authService.refresh("raw");

        assertThat(result.accessToken()).isEqualTo("newAt");
        assertThat(result.expiresIn()).isEqualTo(3600L);
    }

    @Test
    void refresh_expired_token_throws_AuthException() {
        var tokenModel = new RefreshTokenModel(UUID.randomUUID(), UUID.randomUUID(),
            "hash", Instant.now().minusSeconds(1), false, Instant.now());

        when(jwtService.hashToken("raw")).thenReturn("hash");
        when(refreshTokenStore.findByTokenHash("hash")).thenReturn(Optional.of(tokenModel));

        assertThatThrownBy(() -> authService.refresh("raw"))
            .isInstanceOf(AuthException.class);
    }

    @Test
    void refresh_revoked_token_throws_AuthException() {
        var tokenModel = new RefreshTokenModel(UUID.randomUUID(), UUID.randomUUID(),
            "hash", Instant.now().plusSeconds(3600), true, Instant.now());

        when(jwtService.hashToken("raw")).thenReturn("hash");
        when(refreshTokenStore.findByTokenHash("hash")).thenReturn(Optional.of(tokenModel));

        assertThatThrownBy(() -> authService.refresh("raw"))
            .isInstanceOf(AuthException.class);
    }

    @Test
    void refresh_unknown_token_throws_AuthException() {
        when(jwtService.hashToken(anyString())).thenReturn("unknown");
        when(refreshTokenStore.findByTokenHash("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("raw"))
            .isInstanceOf(AuthException.class);
    }

    private UserModel userModel(String email, UserRole role) {
        return new UserModel(UUID.randomUUID(), email, "hash", role, Instant.now(), Instant.now());
    }
}
