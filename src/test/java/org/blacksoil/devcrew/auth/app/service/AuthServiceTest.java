package org.blacksoil.devcrew.auth.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.blacksoil.devcrew.auth.domain.AuthException;
import org.blacksoil.devcrew.auth.domain.OrganizationCreationPort;
import org.blacksoil.devcrew.auth.domain.UserRole;
import org.blacksoil.devcrew.auth.domain.model.RefreshTokenModel;
import org.blacksoil.devcrew.auth.domain.model.UserModel;
import org.blacksoil.devcrew.auth.domain.store.RefreshTokenStore;
import org.blacksoil.devcrew.auth.domain.store.UserStore;
import org.blacksoil.devcrew.common.TimeProvider;
import org.blacksoil.devcrew.common.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

  @Mock private UserStore userStore;

  @Mock private RefreshTokenStore refreshTokenStore;

  @Mock private JwtService jwtService;

  @Mock private OrganizationCreationPort organizationCreationPort;

  private AuthService authService;

  @BeforeEach
  void setUp() {
    authService =
        new AuthService(
            userStore,
            refreshTokenStore,
            jwtService,
            new BCryptPasswordEncoder(),
            organizationCreationPort,
            new TimeProvider());
  }

  // --- register ---

  @Test
  void register_creates_organization_and_assigns_ARCHITECT_role() {
    var orgId = UUID.randomUUID();
    when(organizationCreationPort.createForUser(anyString())).thenReturn(orgId);
    when(userStore.findByEmail(anyString())).thenReturn(Optional.empty());
    when(userStore.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(refreshTokenStore.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(jwtService.generateAccessToken(any(), any(), anyString(), any())).thenReturn("access");
    when(jwtService.generateRefreshToken(any())).thenReturn("refresh");
    when(jwtService.hashToken(anyString())).thenReturn("hash");
    when(jwtService.getAccessTokenTtlSeconds()).thenReturn(3600L);
    when(jwtService.getRefreshTokenTtlSeconds()).thenReturn(604800L);

    authService.register("admin@test.com", "password", null);

    var captor = ArgumentCaptor.<UserModel>captor();
    verify(userStore).save(captor.capture());
    assertThat(captor.getValue().role()).isEqualTo(UserRole.ARCHITECT);
    assertThat(captor.getValue().orgId()).isEqualTo(orgId);
  }

  @Test
  void register_uses_provided_org_name() {
    when(organizationCreationPort.createForUser("Acme Corp")).thenReturn(UUID.randomUUID());
    when(userStore.findByEmail(anyString())).thenReturn(Optional.empty());
    when(userStore.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(refreshTokenStore.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(jwtService.generateAccessToken(any(), any(), anyString(), any())).thenReturn("access");
    when(jwtService.generateRefreshToken(any())).thenReturn("refresh");
    when(jwtService.hashToken(anyString())).thenReturn("hash");
    when(jwtService.getAccessTokenTtlSeconds()).thenReturn(3600L);
    when(jwtService.getRefreshTokenTtlSeconds()).thenReturn(604800L);

    authService.register("admin@test.com", "password", "Acme Corp");

    verify(organizationCreationPort).createForUser("Acme Corp");
  }

  @Test
  void register_duplicate_email_throws_ConflictException() {
    when(userStore.findByEmail("dup@test.com"))
        .thenReturn(Optional.of(userModel("dup@test.com", UserRole.VIEWER)));

    assertThatThrownBy(() -> authService.register("dup@test.com", "password", null))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void register_returns_login_result_with_tokens() {
    when(organizationCreationPort.createForUser(anyString())).thenReturn(UUID.randomUUID());
    when(userStore.findByEmail(anyString())).thenReturn(Optional.empty());
    when(userStore.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(refreshTokenStore.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(jwtService.generateAccessToken(any(), any(), anyString(), any())).thenReturn("myAccess");
    when(jwtService.generateRefreshToken(any())).thenReturn("myRefresh");
    when(jwtService.hashToken("myRefresh")).thenReturn("myHash");
    when(jwtService.getAccessTokenTtlSeconds()).thenReturn(3600L);
    when(jwtService.getRefreshTokenTtlSeconds()).thenReturn(604800L);

    var result = authService.register("admin@test.com", "pass", null);

    assertThat(result.accessToken()).isEqualTo("myAccess");
    assertThat(result.refreshToken()).isEqualTo("myRefresh");
    assertThat(result.expiresIn()).isEqualTo(3600L);
  }

  // --- login ---

  @Test
  void login_valid_credentials_returns_tokens() {
    var encoder = new BCryptPasswordEncoder();
    var user = userModelWithPassword("user@test.com", encoder.encode("secret"), UserRole.ARCHITECT);
    when(userStore.findByEmail("user@test.com")).thenReturn(Optional.of(user));
    when(refreshTokenStore.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(jwtService.generateAccessToken(any(), any(), anyString(), any())).thenReturn("at");
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
    var user = userModelWithPassword("user@test.com", encoder.encode("correct"), UserRole.VIEWER);
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
    var tokenModel =
        new RefreshTokenModel(
            UUID.randomUUID(),
            userId,
            "hash",
            Instant.parse("2099-01-01T00:00:00Z"), // гарантированно в будущем
            false,
            NOW);

    when(jwtService.hashToken("raw")).thenReturn("hash");
    when(refreshTokenStore.findByTokenHash("hash")).thenReturn(Optional.of(tokenModel));
    when(userStore.findById(tokenModel.userId())).thenReturn(Optional.of(user));
    when(jwtService.generateAccessToken(any(), any(), anyString(), any())).thenReturn("newAt");
    when(jwtService.getAccessTokenTtlSeconds()).thenReturn(3600L);

    var result = authService.refresh("raw");

    assertThat(result.accessToken()).isEqualTo("newAt");
    assertThat(result.expiresIn()).isEqualTo(3600L);
  }

  @Test
  void refresh_expired_token_throws_AuthException() {
    var tokenModel =
        new RefreshTokenModel(
            UUID.randomUUID(), UUID.randomUUID(), "hash", NOW.minusSeconds(1), false, NOW);

    when(jwtService.hashToken("raw")).thenReturn("hash");
    when(refreshTokenStore.findByTokenHash("hash")).thenReturn(Optional.of(tokenModel));

    assertThatThrownBy(() -> authService.refresh("raw")).isInstanceOf(AuthException.class);
  }

  @Test
  void refresh_revoked_token_throws_AuthException() {
    var tokenModel =
        new RefreshTokenModel(
            UUID.randomUUID(), UUID.randomUUID(), "hash", NOW.plusSeconds(3600), true, NOW);

    when(jwtService.hashToken("raw")).thenReturn("hash");
    when(refreshTokenStore.findByTokenHash("hash")).thenReturn(Optional.of(tokenModel));

    assertThatThrownBy(() -> authService.refresh("raw")).isInstanceOf(AuthException.class);
  }

  @Test
  void refresh_unknown_token_throws_AuthException() {
    when(jwtService.hashToken(anyString())).thenReturn("unknown");
    when(refreshTokenStore.findByTokenHash("unknown")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.refresh("raw")).isInstanceOf(AuthException.class);
  }

  @Test
  void refresh_throws_AuthException_when_user_not_found() {
    var userId = UUID.randomUUID();
    var tokenModel =
        new RefreshTokenModel(
            UUID.randomUUID(), userId, "hash", Instant.parse("2099-01-01T00:00:00Z"), false, NOW);

    when(jwtService.hashToken("raw")).thenReturn("hash");
    when(refreshTokenStore.findByTokenHash("hash")).thenReturn(Optional.of(tokenModel));
    when(userStore.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.refresh("raw")).isInstanceOf(AuthException.class);
  }

  @Test
  void register_uses_full_email_as_org_name_when_no_at_sign() {
    when(organizationCreationPort.createForUser("noemail's Organization"))
        .thenReturn(UUID.randomUUID());
    when(userStore.findByEmail(anyString())).thenReturn(Optional.empty());
    when(userStore.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(refreshTokenStore.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(jwtService.generateAccessToken(any(), any(), anyString(), any())).thenReturn("access");
    when(jwtService.generateRefreshToken(any())).thenReturn("refresh");
    when(jwtService.hashToken(anyString())).thenReturn("hash");
    when(jwtService.getAccessTokenTtlSeconds()).thenReturn(3600L);
    when(jwtService.getRefreshTokenTtlSeconds()).thenReturn(604800L);

    authService.register("noemail", "password", null);

    verify(organizationCreationPort).createForUser("noemail's Organization");
  }

  private UserModel userModel(String email, UserRole role) {
    return new UserModel(UUID.randomUUID(), UUID.randomUUID(), email, "hash", role, NOW, NOW);
  }

  private UserModel userModelWithPassword(String email, String encodedPassword, UserRole role) {
    return new UserModel(
        UUID.randomUUID(), UUID.randomUUID(), email, encodedPassword, role, NOW, NOW);
  }
}
