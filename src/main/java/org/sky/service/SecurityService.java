package org.sky.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.sky.util.JwtExtractor;
import org.sky.dto.ApiResponse;
import org.sky.repository.SellerRepository;
import org.jboss.logging.Logger;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

@ApplicationScoped
public class SecurityService {

    private static final Logger log = Logger.getLogger(SecurityService.class);

    @Inject
    JwtExtractor jwtExtractor;
    
    @Inject
    SellerRepository sellerRepository;

    /**
     * Valida el token JWT y extrae el userId
     * @param authorization Header de autorización
     * @return Uni<Long> con el userId si es válido, o error si no
     */
    public Uni<Long> validateJwtToken(String authorization) {
        log.info("🔐 SecurityService.validateJwtToken() - Iniciando validación de token");
        log.info("🔐 Authorization header recibido: " + (authorization != null ? authorization.substring(0, Math.min(20, authorization.length())) + "..." : "null"));
        
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("❌ Token de autorización requerido o formato incorrecto");
            return Uni.createFrom().failure(new SecurityException("Token de autorización requerido"));
        }

        try {
            String token = authorization.substring(7); // Remover "Bearer "
            log.info("🔐 Token extraído (primeros 20 chars): " + token.substring(0, Math.min(20, token.length())) + "...");
            
            Long userId = jwtExtractor.extractUserIdFromToken(token);
            log.info("✅ Token válido - UserId extraído: " + userId);
            
            if (userId == null) {
                log.warn("❌ Token inválido - userId es null");
                return Uni.createFrom().failure(new SecurityException("Token inválido"));
            }
            
            return Uni.createFrom().item(userId);
            
        } catch (Exception e) {
            log.error("❌ Error al validar token: " + e.getMessage(), e);
            // Si es un error de constraint violation, propagarlo como tal
            if (e.getMessage() != null && e.getMessage().contains("duplicate key value violates unique constraint")) {
                log.error("❌ Error de duplicado detectado en validateJwtToken");
                return Uni.createFrom().failure(e);
            }
            return Uni.createFrom().failure(new SecurityException("Token inválido: " + e.getMessage()));
        }
    }

    /**
     * Valida que el userId del token coincida con el adminId proporcionado
     * @param authorization Header de autorización
     * @param adminId ID del administrador a validar
     * @return Uni<Long> con el userId si es válido y autorizado
     */
    public Uni<Long> validateAdminAuthorization(String authorization, Long adminId) {
        log.info("🔐 SecurityService.validateAdminAuthorization() - Validando autorización de admin");
        log.info("🔐 AdminId solicitado: " + adminId);
        
        return validateJwtToken(authorization)
                .chain(userId -> {
                    log.info("🔐 Comparando userId del token (" + userId + ") con adminId solicitado (" + adminId + ")");
                    
                    if (!userId.equals(adminId)) {
                        log.warn("❌ No autorizado - userId del token (" + userId + ") no coincide con adminId (" + adminId + ")");
                        return Uni.createFrom().failure(new SecurityException("No autorizado para este adminId"));
                    }
                    
                    log.info("✅ Autorización exitosa - userId coincide con adminId");
                    return Uni.createFrom().item(userId);
                });
    }

    /**
     * Crea una respuesta de error de seguridad
     * @param message Mensaje de error
     * @param statusCode Código de estado HTTP
     * @return Response con el error
     */
    public Response createSecurityErrorResponse(String message, int statusCode) {
        org.sky.dto.ErrorResponse errorResponse = new org.sky.dto.ErrorResponse(
            message,
            "SECURITY_ERROR",
            java.util.Map.of("timestamp", java.time.Instant.now()),
            java.time.Instant.now()
        );
        return Response.status(statusCode).entity(errorResponse).build();
    }

    /**
     * Valida que el userId del token coincida con el sellerId proporcionado
     * @param authorization Header de autorización
     * @param sellerId ID del vendedor a validar
     * @return Uni<Long> con el userId si es válido y autorizado
     */
    @WithTransaction
    public Uni<Long> validateSellerAuthorization(String authorization, Long sellerId) {
        log.info("🔐 SecurityService.validateSellerAuthorization() - Validando autorización de vendedor");
        log.info("🔐 SellerId solicitado: " + sellerId);
        
        return validateJwtToken(authorization)
                .chain(userId -> {
                    log.info("🔐 Validando que el userId del token (" + userId + ") corresponde al sellerId (" + sellerId + ")");
                    
                    // Buscar el Seller por userId para verificar que corresponde al sellerId solicitado
                    return sellerRepository.findByUserId(userId)
                            .chain(seller -> {
                                if (seller == null) {
                                    log.warn("❌ No se encontró Seller para userId: " + userId);
                                    return Uni.createFrom().failure(new SecurityException("Usuario no es un vendedor válido"));
                                }
                                
                                if (!seller.id.equals(sellerId)) {
                                    log.warn("❌ No autorizado - userId (" + userId + ") corresponde a sellerId (" + seller.id + ") pero se solicitó (" + sellerId + ")");
                                    return Uni.createFrom().failure(new SecurityException("No autorizado para este sellerId"));
                                }
                                
                                log.info("✅ Autorización exitosa - userId (" + userId + ") corresponde al sellerId (" + sellerId + ")");
                                return Uni.createFrom().item(userId);
                            });
                });
    }

    /**
     * Maneja excepciones de seguridad y retorna respuesta apropiada
     * @param throwable Excepción capturada
     * @return Response con el error de seguridad
     */
    public Response handleSecurityException(Throwable throwable) {
        log.error("❌ SecurityService.handleSecurityException() - Manejo de excepción de seguridad");
        log.error("❌ Tipo de excepción: " + throwable.getClass().getSimpleName());
        log.error("❌ Mensaje: " + throwable.getMessage());
        
        if (throwable instanceof SecurityException) {
            log.warn("❌ Retornando error 401 - SecurityException");
            return createSecurityErrorResponse(throwable.getMessage(), 401);
        }
        
        // Si es un error de constraint violation (duplicado), crear ErrorResponse específico
        if (throwable.getMessage() != null && throwable.getMessage().contains("duplicate key value violates unique constraint")) {
            log.warn("❌ Error de duplicado detectado en SecurityService");
            return createSecurityErrorResponse("Código de transacción duplicado. La notificación se guardó pero la transacción ya existe en el sistema", 400);
        }
        
        log.error("❌ Retornando error 500 - Excepción no controlada");
        return createSecurityErrorResponse("Error de seguridad: " + throwable.getMessage(), 500);
    }
}
