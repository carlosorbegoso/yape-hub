package org.sky.controller;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.sky.dto.request.branch.BranchCreateRequest;
import org.sky.dto.request.branch.BranchUpdateRequest;
import org.sky.service.branch.BranchService;
import org.sky.service.security.SecurityService;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

@Path("/api/admin/branches")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Branch Management", description = "Branch management endpoints for administrators")
@SecurityRequirement(name = "bearerAuth")
public class BranchController {
    
    @Inject
    BranchService branchService;
    
    @Inject
    SecurityService securityService;
    
    private static final Logger log = Logger.getLogger(BranchController.class);
    
    @POST
    @Operation(summary = "Create new branch", description = "Create a new branch for the authenticated admin")
    @APIResponses(value = {
        @APIResponse(responseCode = "201", description = "Branch created successfully"),
        @APIResponse(responseCode = "400", description = "Invalid input data"),
        @APIResponse(responseCode = "401", description = "Unauthorized"),
        @APIResponse(responseCode = "409", description = "Branch code already exists")
    })
    public Uni<Response> createBranch(@Valid BranchCreateRequest request,
                                      @HeaderParam("Authorization") String authorization) {
        log.info("🏢 BranchController.createBranch() - Creando nueva sucursal");
        log.info("🏢 Nombre: " + request.name());
        log.info("🏢 Código: " + request.code());
        log.info("🏢 Dirección: " + request.address());
        
        // Validar token JWT y obtener adminId
        return securityService.validateJwtToken(authorization)
                .chain(adminId -> {
                    log.info("✅ Token válido para adminId: " + adminId);
                    return branchService.createBranch(adminId, request);
                })
                .map(response -> {
                    log.info("✅ Sucursal creada exitosamente");
                    return Response.status(201).entity(response).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error creando sucursal: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    @GET
    @Operation(summary = "List branches", description = "Get list of branches for the authenticated admin with pagination")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Branches retrieved successfully"),
        @APIResponse(responseCode = "401", description = "Unauthorized")
    })
    public Uni<Response> listBranches(@QueryParam("page") @DefaultValue("0") int page,
                                      @QueryParam("size") @DefaultValue("20") int size,
                                      @QueryParam("status") @DefaultValue("all") String status,
                                      @HeaderParam("Authorization") String authorization) {
        log.info("📋 BranchController.listBranches() - Listando sucursales");
        log.info("📋 Página: " + page + ", Tamaño: " + size + ", Status: " + status);
        
        // Validar token JWT y obtener adminId
        return securityService.validateJwtToken(authorization)
                .chain(adminId -> {
                    log.info("✅ Token válido para adminId: " + adminId);
                    return branchService.listBranches(adminId, page, size, status);
                })
                .map(response -> {
                    log.info("✅ Sucursales listadas exitosamente");
                    return Response.ok(response).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error listando sucursales: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    @GET
    @Path("/{branchId}")
    @Operation(summary = "Get branch details", description = "Get detailed information about a specific branch")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Branch details retrieved successfully"),
        @APIResponse(responseCode = "401", description = "Unauthorized"),
        @APIResponse(responseCode = "404", description = "Branch not found")
    })
    public Uni<Response> getBranch(@PathParam("branchId") Long branchId,
                                   @HeaderParam("Authorization") String authorization) {
        log.info("🔍 BranchController.getBranch() - Obteniendo detalles de sucursal: " + branchId);
        
        // Validar token JWT y obtener adminId
        return securityService.validateJwtToken(authorization)
                .chain(adminId -> {
                    log.info("✅ Token válido para adminId: " + adminId);
                    return branchService.getBranchById(adminId, branchId);
                })
                .map(response -> {
                    log.info("✅ Detalles de sucursal obtenidos exitosamente");
                    return Response.ok(response).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo detalles de sucursal: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    @PUT
    @Path("/{branchId}")
    @Operation(summary = "Update branch", description = "Update an existing branch")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Branch updated successfully"),
        @APIResponse(responseCode = "400", description = "Invalid input data"),
        @APIResponse(responseCode = "401", description = "Unauthorized"),
        @APIResponse(responseCode = "404", description = "Branch not found"),
        @APIResponse(responseCode = "409", description = "Branch code already exists")
    })
    public Uni<Response> updateBranch(@PathParam("branchId") Long branchId,
                                      @Valid BranchUpdateRequest request,
                                      @HeaderParam("Authorization") String authorization) {
        log.info("✏️ BranchController.updateBranch() - Actualizando sucursal: " + branchId);
        log.info("✏️ Nombre: " + request.name());
        log.info("✏️ Código: " + request.code());
        log.info("✏️ Dirección: " + request.address());
        log.info("✏️ Activo: " + request.isActive());
        
        // Validar token JWT y obtener adminId
        return securityService.validateJwtToken(authorization)
                .chain(adminId -> {
                    log.info("✅ Token válido para adminId: " + adminId);
                    return branchService.updateBranch(adminId, branchId, request);
                })
                .map(response -> {
                    log.info("✅ Sucursal actualizada exitosamente");
                    return Response.ok(response).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error actualizando sucursal: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    @DELETE
    @Path("/{branchId}")
    @Operation(summary = "Delete branch", description = "Delete a branch (soft delete by setting isActive to false)")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Branch deleted successfully"),
        @APIResponse(responseCode = "401", description = "Unauthorized"),
        @APIResponse(responseCode = "404", description = "Branch not found"),
        @APIResponse(responseCode = "409", description = "Branch has active sellers")
    })
    public Uni<Response> deleteBranch(@PathParam("branchId") Long branchId,
                                      @HeaderParam("Authorization") String authorization) {
        log.info("🗑️ BranchController.deleteBranch() - Eliminando sucursal: " + branchId);
        
        // Validar token JWT y obtener adminId
        return securityService.validateJwtToken(authorization)
                .chain(adminId -> {
                    log.info("✅ Token válido para adminId: " + adminId);
                    return branchService.deleteBranch(adminId, branchId);
                })
                .map(response -> {
                    log.info("✅ Sucursal eliminada exitosamente");
                    return Response.ok(response).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error eliminando sucursal: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    @GET
    @Path("/{branchId}/sellers")
    @Operation(summary = "Get branch sellers", description = "Get list of sellers for a specific branch")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Branch sellers retrieved successfully"),
        @APIResponse(responseCode = "401", description = "Unauthorized"),
        @APIResponse(responseCode = "404", description = "Branch not found")
    })
    public Uni<Response> getBranchSellers(@PathParam("branchId") Long branchId,
                                          @QueryParam("page") @DefaultValue("0") int page,
                                          @QueryParam("size") @DefaultValue("20") int size,
                                          @HeaderParam("Authorization") String authorization) {
        log.info("👥 BranchController.getBranchSellers() - Obteniendo vendedores de sucursal: " + branchId);
        log.info("👥 Página: " + page + ", Tamaño: " + size);
        
        // Validar token JWT y obtener adminId
        return securityService.validateJwtToken(authorization)
                .chain(adminId -> {
                    log.info("✅ Token válido para adminId: " + adminId);
                    return branchService.getBranchSellers(adminId, branchId, page, size);
                })
                .map(response -> {
                    log.info("✅ Vendedores de sucursal obtenidos exitosamente");
                    return Response.ok(response).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo vendedores de sucursal: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
}
