package org.sky.util.jwt;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.sky.model.UserRole;

import java.time.Duration;
import java.time.Instant;

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

  private JwtClaimsBuilder baseClaims(Long userId) {
    return Jwt.claims()
        .subject(userId.toString())
        .issuer(issuer)
        .issuedAt(Instant.now());
  }

  public String generateAccessToken(Long userId, UserRole role, Long sellerId) {
    JwtClaimsBuilder builder = baseClaims(userId)
        .expiresAt(Instant.now().plus(accessDuration))
        .groups(role.name())
        .claim("type", "access");

    if (role == UserRole.SELLER && sellerId != null) {
      builder.claim("sellerId", sellerId);
    }
    return builder.sign();
  }

  public String generateRefreshToken(Long userId){
    return baseClaims(userId)
        .expiresAt(Instant.now().plus(refreshDuration))
        .claim("type", "refresh")
        .sign();
  }


}
