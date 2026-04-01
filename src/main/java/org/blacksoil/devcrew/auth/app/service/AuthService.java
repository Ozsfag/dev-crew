package org.blacksoil.devcrew.auth.app.service;

import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.auth.domain.*;
import org.blacksoil.devcrew.common.exception.ConflictException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserStore userStore;
    private final RefreshTokenStore refreshTokenStore;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public LoginResult register(String email, String password) {
        if (userStore.findByEmail(email).isPresent()) {
            throw new ConflictException("Пользователь с email '%s' уже существует".formatted(email));
        }
        var role = userStore.existsAny() ? UserRole.VIEWER : UserRole.ARCHITECT;
        var now = Instant.now();
        var user = userStore.save(new UserModel(
            UUID.randomUUID(), email, passwordEncoder.encode(password), role, now, now
        ));
        return issueTokens(user);
    }

    @Transactional
    public LoginResult login(String email, String password) {
        var user = userStore.findByEmail(email)
            .orElseThrow(() -> new AuthException("Неверный email или пароль"));
        if (!passwordEncoder.matches(password, user.passwordHash())) {
            throw new AuthException("Неверный email или пароль");
        }
        return issueTokens(user);
    }

    @Transactional
    public RefreshResult refresh(String rawRefreshToken) {
        var hash = jwtService.hashToken(rawRefreshToken);
        var token = refreshTokenStore.findByTokenHash(hash)
            .orElseThrow(() -> new AuthException("Refresh token не найден"));
        if (token.revoked()) {
            throw new AuthException("Refresh token отозван");
        }
        if (token.expiresAt().isBefore(Instant.now())) {
            throw new AuthException("Refresh token истёк");
        }
        var user = userStore.findById(token.userId())
            .orElseThrow(() -> new AuthException("Пользователь не найден"));
        var accessToken = jwtService.generateAccessToken(user.id(), user.email(), user.role());
        return new RefreshResult(accessToken, jwtService.getAccessTokenTtlSeconds());
    }

    private LoginResult issueTokens(UserModel user) {
        var rawRefresh = jwtService.generateRefreshToken(user.id());
        var tokenHash = jwtService.hashToken(rawRefresh);
        var expiresAt = Instant.now().plusSeconds(jwtService.getRefreshTokenTtlSeconds());
        refreshTokenStore.save(new RefreshTokenModel(
            UUID.randomUUID(), user.id(), tokenHash, expiresAt, false, Instant.now()
        ));
        var accessToken = jwtService.generateAccessToken(user.id(), user.email(), user.role());
        return new LoginResult(accessToken, rawRefresh, jwtService.getAccessTokenTtlSeconds());
    }

    public record LoginResult(String accessToken, String refreshToken, long expiresIn) {}
    public record RefreshResult(String accessToken, long expiresIn) {}
}
