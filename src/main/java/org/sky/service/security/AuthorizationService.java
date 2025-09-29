package org.sky.service.security;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
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
    
    @Inject
    SellerRepository sellerRepository;

    public Uni<Long> validateAdminAuthorization(String authorization, Long adminId) {
        return validateJwtToken(authorization)
                .chain(userId -> validateAdminAccess(userId, adminId));
    }

    public Uni<Long> validateSellerAuthorization(String authorization, Long sellerId) {
        return validateJwtToken(authorization)
                .chain(userId -> validateSellerToken(authorization, sellerId));
    }

    @WithTransaction
    public Uni<Long> validateAdminCanAccessSeller(String authorization, Long adminId, Long sellerId) {
        return validateAdminAuthorization(authorization, adminId)
                .chain(userId -> validateSellerAccess(userId, sellerId));
    }

    private Uni<Long> validateJwtToken(String authorization) {
        return extractToken(authorization)
                .chain(this::validateToken)
                .chain(this::extractUserId);
    }

    private Uni<String> extractToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Uni.createFrom().failure(new SecurityException("Authorization token required"));
        }
        return Uni.createFrom().item(authorization.substring(7));
    }

    private Uni<org.eclipse.microprofile.jwt.JsonWebToken> validateToken(String token) {
        return jwtValidator.isValidAccessToken(token)
                .chain(isValid -> {
                    if (Boolean.FALSE.equals(isValid)) {
                        return Uni.createFrom().failure(new SecurityException("Invalid token"));
                    }
                    return jwtValidator.parseToken(token)
                            .onItem().ifNull().failWith(() -> new SecurityException("Invalid token"));
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

    private Uni<Long> validateSellerAccess(Long userId, Long sellerId) {
        return sellerRepository.findBySellerIdAndAdminId(sellerId, userId)
                .chain(seller -> {
                    if (seller == null) {
                        return Uni.createFrom().failure(new SecurityException("Not authorized to access this seller"));
                    }
                    return Uni.createFrom().item(userId);
                });
    }
}
