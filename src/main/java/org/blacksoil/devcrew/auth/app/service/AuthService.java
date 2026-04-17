package org.blacksoil.devcrew.auth.app.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.auth.domain.AuthException;
import org.blacksoil.devcrew.auth.domain.OrganizationCreationPort;
import org.blacksoil.devcrew.auth.domain.UserRole;
import org.blacksoil.devcrew.auth.domain.model.RefreshTokenModel;
import org.blacksoil.devcrew.auth.domain.model.UserModel;
import org.blacksoil.devcrew.auth.domain.store.RefreshTokenStore;
import org.blacksoil.devcrew.auth.domain.store.UserStore;
import org.blacksoil.devcrew.common.TimeProvider;
import org.blacksoil.devcrew.common.exception.ConflictException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserStore userStore;
  private final RefreshTokenStore refreshTokenStore;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;
  private final OrganizationCreationPort organizationCreationPort;
  private final TimeProvider timeProvider;

  private static String defaultOrgName(String email) {
    var prefix = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
    return prefix + "'s Organization";
  }

  @Transactional
  public LoginResult register(String email, String password, String orgName) {
    if (userStore.findByEmail(email).isPresent()) {
      throw new ConflictException("Пользователь с email '%s' уже существует".formatted(email));
    }
    // Каждый новый пользователь создаёт свою организацию и становится ARCHITECT
    var name = (orgName != null && !orgName.isBlank()) ? orgName : defaultOrgName(email);
    var orgId = organizationCreationPort.createForUser(name);
    var now = timeProvider.now();
    var user =
        userStore.save(
            new UserModel(
                UUID.randomUUID(),
                orgId,
                email,
                passwordEncoder.encode(password),
                UserRole.ARCHITECT,
                now,
                now));
    return issueTokens(user);
  }

  @Transactional
  public LoginResult login(String email, String password) {
    var user =
        userStore
            .findByEmail(email)
            .orElseThrow(() -> new AuthException("Неверный email или пароль"));
    if (!passwordEncoder.matches(password, user.passwordHash())) {
      throw new AuthException("Неверный email или пароль");
    }
    return issueTokens(user);
  }

  @Transactional
  public RefreshResult refresh(String rawRefreshToken) {
    var hash = jwtService.hashToken(rawRefreshToken);
    var token =
        refreshTokenStore
            .findByTokenHash(hash)
            .orElseThrow(() -> new AuthException("Refresh token не найден"));
    if (token.revoked()) {
      throw new AuthException("Refresh token отозван");
    }
    if (token.expiresAt().isBefore(timeProvider.now())) {
      throw new AuthException("Refresh token истёк");
    }
    var user =
        userStore
            .findById(token.userId())
            .orElseThrow(() -> new AuthException("Пользователь не найден"));
    var accessToken =
        jwtService.generateAccessToken(user.id(), user.orgId(), user.email(), user.role());
    return new RefreshResult(accessToken, jwtService.getAccessTokenTtlSeconds());
  }

  private LoginResult issueTokens(UserModel user) {
    var rawRefresh = jwtService.generateRefreshToken(user.id());
    var tokenHash = jwtService.hashToken(rawRefresh);
    var now = timeProvider.now();
    var expiresAt = now.plusSeconds(jwtService.getRefreshTokenTtlSeconds());
    refreshTokenStore.save(
        new RefreshTokenModel(UUID.randomUUID(), user.id(), tokenHash, expiresAt, false, now));
    var accessToken =
        jwtService.generateAccessToken(user.id(), user.orgId(), user.email(), user.role());
    return new LoginResult(accessToken, rawRefresh, jwtService.getAccessTokenTtlSeconds());
  }

  @Transactional
  public void logout(String rawRefreshToken) {
    var hash = jwtService.hashToken(rawRefreshToken);
    var token =
        refreshTokenStore
            .findByTokenHash(hash)
            .orElseThrow(() -> new AuthException("Refresh token не найден"));
    refreshTokenStore.save(
        new RefreshTokenModel(
            token.id(),
            token.userId(),
            token.tokenHash(),
            token.expiresAt(),
            true,
            token.createdAt()));
  }

  public record LoginResult(String accessToken, String refreshToken, long expiresIn) {}

  public record RefreshResult(String accessToken, long expiresIn) {}
}
