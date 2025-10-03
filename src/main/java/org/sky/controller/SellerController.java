package org.sky.controller;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.annotation.security.PermitAll;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import org.sky.service.SellerService;
import org.sky.service.SubscriptionService;
import org.sky.service.security.SecurityService;
import org.sky.repository.SellerRepository;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Path("/api/admin/sellers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Seller Management", description = "Seller management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class SellerController {
    
    @Inject
    SellerService sellerService;
    
    @Inject
    SubscriptionService subscriptionService;
    
    @Inject
    SellerRepository sellerRepository;
    
    @Inject
    SecurityService securityService;
    
    @Inject
    JsonWebToken jwt;
    
    private static final Logger log = Logger.getLogger(SellerController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @GET
    @Path("/my-sellers")
    @PermitAll
    @WithTransaction
    @Operation(summary = "Get my sellers", description = "Get all sellers affiliated to the current admin with pagination")
    public Uni<Response> getMySellers(@QueryParam("adminId") Long adminId,
                                     @QueryParam("startDate") String startDateStr,
                                     @QueryParam("endDate") String endDateStr,
                                     @QueryParam("page") @DefaultValue("1") int page,
                                     @QueryParam("limit") @DefaultValue("20") int limit,
                                     @HeaderParam("Authorization") String authorization) {
        log.info("🚀 SellerController.getMySellers() - Endpoint llamado para adminId: " + adminId);
        log.info("🚀 Parámetros de paginación - page: " + page + ", limit: " + limit);
        log.info("🚀 Desde: " + startDateStr + ", Hasta: " + endDateStr);
        
        // Validar parámetros de fecha
        final LocalDate startDate, endDate;
        try {
            if (startDateStr != null && endDateStr != null) {
                startDate = LocalDate.parse(startDateStr, DATE_FORMATTER);
                endDate = LocalDate.parse(endDateStr, DATE_FORMATTER);
            } else {
                // Default: último mes
                endDate = LocalDate.now();
                startDate = endDate.minusDays(30);
            }
        } catch (DateTimeParseException e) {
            log.warn("❌ Fechas inválidas: " + e.getMessage());
            return Uni.createFrom().item(Response.status(400)
                    .entity(org.sky.dto.response.ApiResponse.error("Formato de fecha inválido. Use yyyy-MM-dd")).build());
        }
        
        // Validar autorización de admin
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> {
                    log.info("✅ Autorización exitosa para adminId: " + adminId);
                    return sellerService.getSellersByAdmin(adminId, page, limit, startDate, endDate);
                })
                .map(response -> {
                    if (response.isSuccess()) {
                        log.info("✅ Retornando lista de vendedores exitosamente");
                        return Response.ok(response).build();
                    } else {
                        log.warn("⚠️ Error al obtener lista de vendedores: " + response.message());
                        return Response.status(400).entity(response).build();
                    }
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error de autorización: " + throwable.getMessage());
                    // Si es una ValidationException, crear ErrorResponse manualmente
                    if (throwable instanceof org.sky.exception.ValidationException) {
                        org.sky.exception.ValidationException validationException = (org.sky.exception.ValidationException) throwable;
                        org.sky.dto.response.ErrorResponse errorResponse = new org.sky.dto.response.ErrorResponse(
                            validationException.getMessage(),
                            validationException.getErrorCode(),
                            validationException.getDetails(),
                            java.time.Instant.now()
                        );
                        return Response.status(validationException.getStatus()).entity(errorResponse).build();
                    }
                    // Para otros errores, usar el manejo de seguridad
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    @GET
    @Operation(summary = "List sellers", description = "Get list of sellers with pagination")
    public Uni<Response> listSellers(@QueryParam("page") @DefaultValue("1") int page,
                               @QueryParam("limit") @DefaultValue("20") int limit,
                               @QueryParam("branchId") Long branchId,
                               @QueryParam("status") @DefaultValue("all") String status) {
        try {
            Long userId = getCurrentUserId();
            return sellerService.listSellers(userId, page, limit, branchId, status)
                    .map(response -> {
                        if (response.isSuccess()) {
                            return Response.ok(response).build();
                        } else {
                            return Response.status(400).entity(response).build();
                        }
                    });
        } catch (SecurityException e) {
            log.warn("Authentication failed in listSellers: " + e.getMessage());
            return Uni.createFrom()
                    .item(Response.status(401)
                            .entity(org.sky.dto.response.ApiResponse.error(e.getMessage()))
                            .build());
        }
    }
    
    @PUT
    @Path("/{sellerId}")
    @PermitAll
    @WithTransaction
    @Operation(summary = "Update seller", description = "Update seller information")
    public Uni<Response> updateSeller(@QueryParam("adminId") Long adminId,
                                @PathParam("sellerId") Long sellerId,
                                @QueryParam("name") String name,
                                @QueryParam("phone") String phone,
                                @QueryParam("isActive") Boolean isActive,
                                @HeaderParam("Authorization") String authorization) {
        log.info("🚀 SellerController.updateSeller() - Endpoint llamado para sellerId: " + sellerId + ", adminId: " + adminId);
        
        // Validar autorización de admin primero
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> {
                    log.info("✅ Autorización exitosa para adminId: " + adminId);
                    return sellerService.updateSeller(adminId, sellerId, name, phone, isActive);
                })
                .map(response -> {
                    log.info("✅ Vendedor actualizado exitosamente");
                    return Response.ok(response).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error en actualización: " + throwable.getMessage());
                    // Si es una ValidationException, crear ErrorResponse manualmente
                    if (throwable instanceof org.sky.exception.ValidationException) {
                        org.sky.exception.ValidationException validationException = (org.sky.exception.ValidationException) throwable;
                        org.sky.dto.response.ErrorResponse errorResponse = new org.sky.dto.response.ErrorResponse(
                            validationException.getMessage(),
                            validationException.getErrorCode(),
                            validationException.getDetails(),
                            java.time.Instant.now()
                        );
                        return Response.status(validationException.getStatus()).entity(errorResponse).build();
                    }
                    // Para otros errores, usar el manejo de seguridad
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    @DELETE
    @Path("/{sellerId}")
    @PermitAll
    @WithTransaction
    @Operation(summary = "Delete/Pause seller", description = "Delete or pause a seller (soft delete by default)")
    public Uni<Response> deleteSeller(@QueryParam("adminId") Long adminId,
                                @PathParam("sellerId") Long sellerId,
                                @QueryParam("action") @DefaultValue("pause") String action,
                                @QueryParam("reason") String reason,
                                @HeaderParam("Authorization") String authorization) {
        log.info("🚀 SellerController.deleteSeller() - Endpoint llamado para sellerId: " + sellerId + ", adminId: " + adminId + ", action: " + action);
        
        // Validar autorización de admin
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> {
                    log.info("✅ Autorización exitosa para adminId: " + adminId);
                    return sellerService.deleteSeller(adminId, sellerId, action, reason);
                })
                .map(response -> {
                    log.info("✅ Vendedor procesado exitosamente");
                    return Response.ok(response).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error en eliminación/pausa: " + throwable.getMessage());
                    // Si es una ValidationException, crear ErrorResponse manualmente
                    if (throwable instanceof org.sky.exception.ValidationException) {
                        org.sky.exception.ValidationException validationException = (org.sky.exception.ValidationException) throwable;
                        org.sky.dto.response.ErrorResponse errorResponse = new org.sky.dto.response.ErrorResponse(
                            validationException.getMessage(),
                            validationException.getErrorCode(),
                            validationException.getDetails(),
                            java.time.Instant.now()
                        );
                        return Response.status(validationException.getStatus()).entity(errorResponse).build();
                    }
                    // Para otros errores, usar el manejo de seguridad
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    @GET
    @Path("/limits")
    @PermitAll
    @WithTransaction
    @Operation(summary = "Get seller limits", description = "Get current seller limits based on subscription plan")
    public Uni<Response> getSellerLimits(@QueryParam("adminId") Long adminId,
                                        @HeaderParam("Authorization") String authorization) {
        log.info("🚀 SellerController.getSellerLimits() - Endpoint llamado para adminId: " + adminId);
        
        // Validar autorización de admin
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> {
                    log.info("✅ Autorización exitosa para adminId: " + adminId);
                    return subscriptionService.getSellerLimitsInfo(adminId);
                })
                .chain(limitsInfo -> {
                    // Obtener el número actual de vendedores usando el repositorio directamente
                    return sellerRepository.findByAdminId(adminId)
                            .map(sellers -> {
                                int currentSellers = sellers != null ? sellers.size() : 0;
                                
                                var response = java.util.Map.of(
                                    "adminId", adminId,
                                    "planName", limitsInfo.planName(),
                                    "maxSellers", limitsInfo.maxSellers(),
                                    "currentSellers", currentSellers,
                                    "remainingSlots", Math.max(0, limitsInfo.maxSellers() - currentSellers),
                                    "isActive", limitsInfo.isActive(),
                                    "canAddMore", currentSellers < limitsInfo.maxSellers(),
                                    "timestamp", java.time.LocalDateTime.now()
                                );
                                
                                return Response.ok(org.sky.dto.response.ApiResponse.success("Límites de vendedores obtenidos exitosamente", response)).build();
                            });
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error al obtener límites: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }

    // Helper method to get current user ID from JWT
    private Long getCurrentUserId() {
        if (jwt != null && jwt.getSubject() != null) {
            try {
                return Long.parseLong(jwt.getSubject());
            } catch (NumberFormatException e) {
                log.warn("Invalid user ID format in JWT subject: " + jwt.getSubject());
                throw new SecurityException("Invalid user authentication data");
            }
        }
        log.warn("No JWT token or subject found in request");
        throw new SecurityException("No authenticated user found - valid authorization token required");
    }
}
