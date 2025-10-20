package org.sky.controller;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.sky.dto.request.admin.UpdateAdminProfileRequest;
import org.sky.service.AdminService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

@Path("/api/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Admin Management", description = "Administrator management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {
    
    @Inject
    AdminService adminService;
    @GET
    @Path("/profile")
    @Operation(summary = "Get admin profile", description = "Retrieve administrator profile information")
    public Uni<Response> getAdminProfile(@QueryParam("userId") Long userId) {
        return adminService.getAdminProfile(userId)
                .map(response -> {
                    if (response.isSuccess()) {
                        return Response.ok(response).build();
                    } else {
                        return Response.status(404).entity(response).build();
                    }
                });
    }
    
    @PUT
    @Path("/profile")
    @WithTransaction
    @Operation(summary = "Update admin profile", description = "Update administrator profile information")
    public Uni<Response> updateAdminProfile(@QueryParam("userId") Long userId, 
                                      @Valid UpdateAdminProfileRequest request) {
        return adminService.updateAdminProfile(userId, request)
                .map(response -> {
                    if (response.isSuccess()) {
                        return Response.ok(response).build();
                    } else {
                        return Response.status(400).entity(response).build();
                    }
                });
    }
}
