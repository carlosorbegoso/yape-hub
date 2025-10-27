package org.sky.util.jwt;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.sky.model.UserRole;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

@ApplicationScoped
public class JwtGenerator {
  private final String issuer;
  private final String secret;
  private final Duration accessDuration;
  private final Duration refreshDuration;

  public JwtGenerator(
      @ConfigProperty(name = "jwt.issuer")
      String issuer,
      @ConfigProperty(name = "jwt.secret")
      String secret,
      @ConfigProperty(name = "jwt.access-token.duration")
      Duration accessDuration,
      @ConfigProperty(name = "jwt.refresh-token.duration")
      Duration refreshDuration) {
    this.issuer = issuer;
    this.secret = secret;
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
    try {
      JwtClaimsBuilder builder = baseClaims(userId)
          .expiresAt(Instant.now().plus(accessDuration))
          .groups(role.name())
          .claim("type", "access");

      if (role == UserRole.SELLER && sellerId != null) {
        builder.claim("sellerId", sellerId);
      }
      
      // Use the secret key directly with HMAC256
      SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
      return builder.jws().keyId("jwt-key").sign(secretKey);
    } catch (Exception e) {
      // Fallback to simple signing
      return baseClaims(userId)
          .expiresAt(Instant.now().plus(accessDuration))
          .groups(role.name())
          .claim("type", "access")
          .sign();
    }
  }

  public String generateRefreshToken(Long userId){
    try {
      SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
      return baseClaims(userId)
          .expiresAt(Instant.now().plus(refreshDuration))
          .claim("type", "refresh")
          .jws().keyId("jwt-key").sign(secretKey);
    } catch (Exception e) {
      // Fallback to simple signing
      return baseClaims(userId)
          .expiresAt(Instant.now().plus(refreshDuration))
          .claim("type", "refresh")
          .sign();
    }
  }
}
