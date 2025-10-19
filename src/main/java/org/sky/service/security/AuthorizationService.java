package org.sky.service.security;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.repository.SellerRepository;
import org.sky.util.jwt.JwtExtractor;
import org.sky.util.jwt.JwtValidator;

@ApplicationScoped
public class AuthorizationService {

    @Inject
    JwtValidator jwtValidator;
    
    @Inject
    JwtExtractor jwtExtractor;


    public Uni<Long> validateAdminAuthorization(String authorization, Long adminId) {
        return validateJwtToken(authorization)
                .chain(userId -> validateAdminAccess(userId, adminId));
    }

    public Uni<Long> validateSellerAuthorization(String authorization, Long sellerId) {
        return validateJwtToken(authorization)
                .chain(userId -> validateSellerToken(authorization, sellerId));
    }

  private Uni<Long> validateJwtToken(String authorization) {
        return extractToken(authorization)
                .chain(this::validateToken)
                .chain(this::extractUserId);
    }

    private Uni<String> extractToken(String authorization) {
        if (authorization == null || authorization.trim().isEmpty()) {
            return Uni.createFrom().failure(new SecurityException("Authorization header is missing"));
        }
        if (!authorization.startsWith("Bearer ")) {
            return Uni.createFrom().failure(new SecurityException("Authorization header must start with 'Bearer '"));
        }
        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            return Uni.createFrom().failure(new SecurityException("Token is empty"));
        }
        return Uni.createFrom().item(token);
    }

    private Uni<org.eclipse.microprofile.jwt.JsonWebToken> validateToken(String token) {
        return jwtValidator.isValidAccessToken(token)
                .chain(isValid -> {
                    if (Boolean.FALSE.equals(isValid)) {
                        return Uni.createFrom().failure(new SecurityException("Invalid access token - may be expired or malformed"));
                    }
                    return jwtValidator.parseToken(token)
                            .onItem().ifNull().failWith(() -> new SecurityException("Unable to parse token - invalid format"));
                });
    }

    private Uni<Long> extractUserId(org.eclipse.microprofile.jwt.JsonWebToken jwt) {
        return jwtExtractor.extractUserId(jwt)
                .onItem().ifNull().failWith(() -> new SecurityException("Invalid token - userId not found"));
    }

    private Uni<Long> validateAdminAccess(Long userId, Long adminId) {
        if (adminId != null && !userId.equals(adminId)) {
            return Uni.createFrom().failure(new SecurityException("Not authorized for this adminId"));
        }
        return Uni.createFrom().item(userId);
    }

    private Uni<Long> validateSellerToken(String authorization, Long sellerId) {
        return extractToken(authorization)
                .chain(this::validateToken)
                .chain(jwt -> extractUserAndSellerIds(jwt, sellerId));
    }

    private Uni<Long> extractUserAndSellerIds(org.eclipse.microprofile.jwt.JsonWebToken jwt, Long sellerId) {
        return jwtExtractor.extractUserId(jwt)
                .chain(userId -> jwtExtractor.extractSellerId(jwt)
                        .chain(tokenSellerId -> validateSellerAccess(userId, tokenSellerId, sellerId)));
    }

    private Uni<Long> validateSellerAccess(Long userId, Long tokenSellerId, Long sellerId) {
        if (userId == null) {
            return Uni.createFrom().failure(new SecurityException("Invalid token"));
        }
        
        if (tokenSellerId == null) {
            return Uni.createFrom().failure(new SecurityException("Invalid token - missing sellerId"));
        }
        
        if (!tokenSellerId.equals(sellerId)) {
            return Uni.createFrom().failure(new SecurityException("Not authorized for this sellerId"));
        }
        
        return Uni.createFrom().item(userId);
    }

}
