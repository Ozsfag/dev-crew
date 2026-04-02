package org.blacksoil.devcrew.auth.app.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.auth.app.config.JwtProperties;
import org.blacksoil.devcrew.auth.domain.UserRole;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_ORG_ID = "orgId";

    private final JwtProperties properties;

    public String generateAccessToken(UUID userId, UUID orgId, String email, UserRole role) {
        var now = Instant.now();
        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .claim(CLAIM_ROLE, role.name())
            .claim(CLAIM_ORG_ID, orgId != null ? orgId.toString() : null)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(properties.getAccessTokenTtlSeconds())))
            .signWith(signingKey())
            .compact();
    }

    public String generateRefreshToken(UUID userId) {
        var now = Instant.now();
        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(properties.getRefreshTokenTtlSeconds())))
            .signWith(signingKey())
            .compact();
    }

    public void validateAccessToken(String token) {
        Jwts.parser()
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token);
    }

    public UUID extractUserId(String token) {
        var claims = Jwts.parser()
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return UUID.fromString(claims.getSubject());
    }

    public UserRole extractRole(String token) {
        var claims = Jwts.parser()
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return UserRole.valueOf(claims.get(CLAIM_ROLE, String.class));
    }

    public UUID extractOrgId(String token) {
        var claims = Jwts.parser()
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
        var orgIdStr = claims.get(CLAIM_ORG_ID, String.class);
        return orgIdStr != null ? UUID.fromString(orgIdStr) : null;
    }

    public String hashToken(String rawToken) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var bytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 недоступен", e);
        }
    }

    public long getAccessTokenTtlSeconds() {
        return properties.getAccessTokenTtlSeconds();
    }

    public long getRefreshTokenTtlSeconds() {
        return properties.getRefreshTokenTtlSeconds();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
