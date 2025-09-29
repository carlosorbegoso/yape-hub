package org.sky.util.jwt;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Optional;

@ApplicationScoped
public class JwtExtractor {

  public Uni<Long> extractUserId(JsonWebToken jwt) {
    return Uni.createFrom().item(() -> Optional.ofNullable(jwt.getSubject())
        .map(Long::parseLong)
        .orElse(null));

  }

  public Uni<Long> extractSellerId(JsonWebToken jwt) {
    return extractLongClaim(jwt, "sellerId");
  }

  public Uni<Long> extractLongClaim(JsonWebToken jwt, String claimName) {
    return Uni.createFrom().item(() -> {
      Object claim = jwt.getClaim(claimName);
      
      if (claim instanceof Number n) {
        return n.longValue();
      }
      if (claim instanceof String s) {
        try {
          return Long.parseLong(s);
        } catch (NumberFormatException ignored) {
          // Invalid string format
        }
      }
      // Handle JsonLongNumber and other numeric types
      if (claim != null) {
        try {
          String claimStr = claim.toString();
          return Long.parseLong(claimStr);
        } catch (NumberFormatException ignored) {
          // Invalid format
        }
      }
      return null;
    });
  }

}
