package org.sky.controller;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.annotation.security.PermitAll;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import org.sky.dto.seller.AffiliateSellerRequest;
import org.sky.service.SellerService;
import org.sky.service.SecurityService;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;

@Path("/api/admin/sellers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Seller Management", description = "Seller management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class SellerController {
    
    @Inject
    SellerService sellerService;
    
    @Inject
    SecurityService securityService;
    
    @Inject
    JsonWebToken jwt;
    
    private static final Logger log = Logger.getLogger(SellerController.class);
    
    @GET
    @Path("/my-sellers")
    @PermitAll
    @WithTransaction
    @Operation(summary = "Get my sellers", description = "Get all sellers affiliated to the current admin with pagination")
    public Uni<Response> getMySellers(@QueryParam("adminId") Long adminId, 
                                     @QueryParam("page") @DefaultValue("1") int page,
                                     @QueryParam("limit") @DefaultValue("20") int limit,
                                     @HeaderParam("Authorization") String authorization) {
        log.info("🚀 SellerController.getMySellers() - Endpoint llamado para adminId: " + adminId);
        log.info("🚀 Parámetros de paginación - page: " + page + ", limit: " + limit);
        
        // Validar autorización de admin
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> {
                    log.info("✅ Autorización exitosa para adminId: " + adminId);
                    return sellerService.getSellersByAdmin(adminId, page, limit);
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
                        org.sky.dto.ErrorResponse errorResponse = new org.sky.dto.ErrorResponse(
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
        return sellerService.listSellers(getCurrentUserId(), page, limit, branchId, status)
                .map(response -> {
                    if (response.isSuccess()) {
                        return Response.ok(response).build();
                    } else {
                        return Response.status(400).entity(response).build();
                    }
                });
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
                        org.sky.dto.ErrorResponse errorResponse = new org.sky.dto.ErrorResponse(
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
                        org.sky.dto.ErrorResponse errorResponse = new org.sky.dto.ErrorResponse(
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
    
    // Helper method to get current user ID from JWT
    private Long getCurrentUserId() {
        if (jwt != null && jwt.getSubject() != null) {
            return Long.parseLong(jwt.getSubject());
        }
        throw new SecurityException("No authenticated user found");
    }
}
