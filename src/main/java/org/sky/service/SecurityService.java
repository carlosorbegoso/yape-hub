package org.sky.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.sky.util.jwt.JwtExtractor;
import org.sky.util.jwt.JwtValidator;
import org.sky.repository.SellerRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

@ApplicationScoped
public class SecurityService {

    @Inject
    JwtExtractor jwtExtractor;
    
    @Inject
    JwtValidator jwtValidator;
    
    @Inject
    SellerRepository sellerRepository;

    public Uni<Long> validateJwtToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Uni.createFrom().failure(new SecurityException("Token de autorización requerido"));
        }

        String token = authorization.substring(7);
        
        return jwtValidator.isValidAccessToken(token)
            .onItem().transformToUni(isValid -> {
                if (!isValid) {
                    return Uni.createFrom().failure(new SecurityException("Token inválido"));
                }
                
                return jwtValidator.parseToken(token)
                    .onItem().ifNull().failWith(() -> new SecurityException("Token inválido"))
                    .chain(jwtExtractor::extractUserId)
                    .onItem().ifNull().failWith(() -> new SecurityException("Token inválido - userId no encontrado"));
            });
    }

    public Uni<Long> validateAdminAuthorization(String authorization, Long adminId) {
        return validateJwtToken(authorization)
                .chain(userId -> {
                    if (!userId.equals(adminId)) {
                        return Uni.createFrom().failure(new SecurityException("No autorizado para este adminId"));
                    }
                    return Uni.createFrom().item(userId);
                });
    }

    public Response createSecurityErrorResponse(String message, int statusCode) {
        org.sky.dto.ErrorResponse errorResponse = new org.sky.dto.ErrorResponse(
            message,
            "SECURITY_ERROR",
            java.util.Map.of("timestamp", java.time.Instant.now()),
            java.time.Instant.now()
        );
        return Response.status(statusCode).entity(errorResponse).build();
    }

    public Uni<Long> validateSellerAuthorization(String authorization, Long sellerId) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Uni.createFrom().failure(new SecurityException("Token de autorización requerido"));
        }

        String token = authorization.substring(7);
        
        return jwtValidator.isValidAccessToken(token)
            .onItem().transformToUni(isValid -> {
                if (!isValid) {
                    return Uni.createFrom().failure(new SecurityException("Token inválido"));
                }
                
                return jwtValidator.parseToken(token)
                    .onItem().ifNull().failWith(() -> new SecurityException("Token inválido"))
                    .chain(jwt -> {
                        return jwtExtractor.extractUserId(jwt)
                            .chain(userId -> jwtExtractor.extractSellerId(jwt)
                                .chain(tokenSellerId -> {
                                    if (userId == null) {
                                        return Uni.createFrom().failure(new SecurityException("Token inválido"));
                                    }
                                    
                                    if (tokenSellerId == null) {
                                        return Uni.createFrom().failure(new SecurityException("Token inválido - falta sellerId"));
                                    }
                                    
                                    if (!tokenSellerId.equals(sellerId)) {
                                        return Uni.createFrom().failure(new SecurityException("No autorizado para este sellerId"));
                                    }
                                    
                                    return Uni.createFrom().item(userId);
                                })
                            );
                    });
            });
    }

    @WithTransaction
    public Uni<Long> validateAdminCanAccessSeller(String authorization, Long adminId, Long sellerId) {
        return validateAdminAuthorization(authorization, adminId)
                .chain(userId -> {
                    return sellerRepository.findBySellerIdAndAdminId(sellerId, adminId)
                            .chain(seller -> {
                                if (seller == null) {
                                    return Uni.createFrom().failure(new SecurityException("No autorizado para acceder a este vendedor"));
                                }
                                
                                return Uni.createFrom().item(userId);
                            });
                });
    }

    public Response handleSecurityException(Throwable throwable) {
        if (throwable instanceof SecurityException) {
            return createSecurityErrorResponse(throwable.getMessage(), 401);
        }
        
        if (throwable.getMessage() != null && throwable.getMessage().contains("duplicate key value violates unique constraint")) {
            return createSecurityErrorResponse("Código de transacción duplicado. La notificación se guardó pero la transacción ya existe en el sistema", 400);
        }
        
        return createSecurityErrorResponse("Error de seguridad: " + throwable.getMessage(), 500);
    }
}
