package org.sky.util.jwt;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.sky.model.UserRole;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class JwtGenerator {
    private final String issuer;
    private final Duration accessDuration;
    private final Duration refreshDuration;

    public JwtGenerator(
            @ConfigProperty(name = "jwt.issuer")
            String issuer,
            @ConfigProperty(name = "jwt.access-token.duration")
            Duration accessDuration,
            @ConfigProperty(name = "jwt.refresh-token.duration")
            Duration refreshDuration) {
        this.issuer = issuer;
        this.accessDuration = accessDuration;
        this.refreshDuration = refreshDuration;
    }

    public String generateAccessToken(Long userId, UserRole role, Long sellerId) {
        Instant now = Instant.now();
        Instant expiration = now.plus(accessDuration);

        var builder = Jwt.issuer(issuer)
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(expiration)
                .claim("jti", UUID.randomUUID().toString())
                .groups(role.name())
                .claim("type", "access");

        if (role == UserRole.SELLER && sellerId != null) {
            builder.claim("sellerId", sellerId);
        }

        return builder.sign();
    }

    public String generateRefreshToken(Long userId) {
        Instant now = Instant.now();
        Instant expiration = now.plus(refreshDuration);

        return Jwt.issuer(issuer)
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(expiration)
                .claim("jti", UUID.randomUUID().toString())
                .claim("type", "refresh")
                .sign();
    }
}