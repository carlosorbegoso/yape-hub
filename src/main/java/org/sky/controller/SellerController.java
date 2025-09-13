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
    @Operation(summary = "Get my sellers", description = "Get all sellers affiliated to the current admin")
    public Uni<Response> getMySellers(@QueryParam("adminId") Long adminId, 
                                     @HeaderParam("Authorization") String authorization) {
        log.info("🚀 SellerController.getMySellers() - Endpoint llamado para adminId: " + adminId);
        
        // Validar autorización de admin
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> {
                    log.info("✅ Autorización exitosa para adminId: " + adminId);
                    return sellerService.getSellersByAdmin(adminId);
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
    @Operation(summary = "Update seller", description = "Update seller information")
    public Uni<Response> updateSeller(@QueryParam("adminId") Long adminId,
                                @PathParam("sellerId") Long sellerId,
                                @QueryParam("name") String name,
                                @QueryParam("phone") String phone,
                                @QueryParam("isActive") Boolean isActive) {
        return sellerService.updateSeller(adminId, sellerId, name, phone, isActive)
                .map(response -> {
                    if (response.isSuccess()) {
                        return Response.ok(response).build();
                    } else {
                        return Response.status(400).entity(response).build();
                    }
                });
    }
    
    @DELETE
    @Path("/{sellerId}")
    @Operation(summary = "Delete/Pause seller", description = "Delete or pause a seller")
    public Uni<Response> deleteSeller(@QueryParam("adminId") Long adminId,
                                @PathParam("sellerId") Long sellerId,
                                @QueryParam("action") @DefaultValue("pause") String action,
                                @QueryParam("reason") String reason) {
        return sellerService.deleteSeller(adminId, sellerId, action, reason)
                .map(response -> {
                    if (response.isSuccess()) {
                        return Response.ok(response).build();
                    } else {
                        return Response.status(400).entity(response).build();
                    }
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
