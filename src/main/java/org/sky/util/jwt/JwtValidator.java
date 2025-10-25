package org.sky.util.jwt;


import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

@ApplicationScoped
public class JwtValidator {

  private static final Logger log = Logger.getLogger(JwtValidator.class);
  private final JWTParser jwtParser;

  public JwtValidator(JWTParser jwtParser) {
    this.jwtParser = jwtParser;
  }


  public Uni<JsonWebToken> parseToken(String token) {
    return  Uni.createFrom().item(() -> {
          try {
            return jwtParser.parse(token);
          } catch (ParseException e) {
            log.warnf("Invalid JWT token: %s", e.getMessage());
            throw new SecurityException("Invalid JWT token");
          }
        });
  }

  public Uni<Boolean> isValidAccessToken(String token) {
    return parseToken(token)
        .map(jwt -> "access".equals(jwt.getClaim("type")))
        .onFailure().recoverWithItem(false);
  }

  public Uni<Boolean> isValidRefreshToken(String token) {
    return parseToken(token)
        .map(jwt -> "refresh".equals(jwt.getClaim("type")))
        .onFailure().recoverWithItem(false);
  }
}
