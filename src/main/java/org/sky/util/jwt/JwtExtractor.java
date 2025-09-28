package org.sky.util.jwt;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Optional;
import org.sky.model.User;

import java.util.*;
import java.util.stream.Collectors;

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
      if (claim instanceof Number n) return n.longValue();
      if (claim instanceof String s) {
        try {
          return Long.parseLong(s);
        } catch (NumberFormatException ignored) {
          Log.debug("Invalid claim name: " + claimName);
        }
      }
      return null;
    });
  }

  public Uni<Set<User.UserRole>> extractRoles(JsonWebToken jwt) {
    return Uni.createFrom().item(() ->
        jwt.getGroups().stream()
            .map(r -> {
              try {
                return User.UserRole.valueOf(r.toUpperCase());
              } catch (IllegalArgumentException e) {
                Log.debug("Invalid role: " + r.toUpperCase());
                return null;
              }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet())
    );
  }
}
