package org.sky.service.auth;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.model.User;
import org.sky.util.jwt.JwtGenerator;

@ApplicationScoped
public class TokenService {

    @Inject
    JwtGenerator jwtGenerator;

    public Uni<TokenData> generateTokens(User user) {
        return Uni.createFrom().item(() -> {
            String accessToken = jwtGenerator.generateAccessToken(user.id, user.role, null);
            String refreshToken = jwtGenerator.generateRefreshToken(user.id);
            return new TokenData(user, accessToken, refreshToken);
        });
    }
    
    public Uni<TokenData> generateTokensForSeller(User user, Long sellerId) {
        return Uni.createFrom().item(() -> {
            String accessToken = jwtGenerator.generateAccessToken(user.id, user.role, sellerId);
            String refreshToken = jwtGenerator.generateRefreshToken(user.id);
            return new TokenData(user, accessToken, refreshToken);
        });
    }

  public record TokenData(User user, String accessToken, String refreshToken) {}
}
