package org.sky.controller;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.sky.dto.ApiResponse;
import org.sky.dto.dashboard.AdminDashboardResponse;
import org.sky.dto.dashboard.SellerDashboardResponse;
import org.sky.service.DashboardService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Dashboard", description = "Dashboard and analytics endpoints")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {
    
    @Inject
    DashboardService dashboardService;
    
    @GET
    @Path("/admin/dashboard")
    @Operation(summary = "Get admin dashboard", description = "Get administrator dashboard with summary statistics")
    public Uni<Response> getAdminDashboard(@QueryParam("adminId") Long adminId,
                                    @QueryParam("period") @DefaultValue("month") String period,
                                    @QueryParam("branchId") Long branchId) {
        return dashboardService.getAdminDashboard(adminId, period, branchId)
                .map(response -> {
                    if (response.isSuccess()) {
                        return Response.ok(response).build();
                    } else {
                        return Response.status(400).entity(response).build();
                    }
                });
    }
    
    @GET
    @Path("/sellers/dashboard")
    @Operation(summary = "Get seller dashboard", description = "Get seller dashboard with personal statistics")
    public Uni<Response> getSellerDashboard(@QueryParam("sellerId") Long sellerId,
                                      @QueryParam("period") @DefaultValue("month") String period) {
        return dashboardService.getSellerDashboard(sellerId, period)
                .map(response -> {
                    if (response.isSuccess()) {
                        return Response.ok(response).build();
                    } else {
                        return Response.status(400).entity(response).build();
                    }
                });
    }
    
    @GET
    @Path("/admin/analytics/reports")
    @Operation(summary = "Get analytics reports", description = "Get detailed analytics reports")
    public Response getAnalyticsReports(@QueryParam("adminId") Long adminId,
                                       @QueryParam("type") @DefaultValue("monthly") String type,
                                       @QueryParam("startDate") String startDate,
                                       @QueryParam("endDate") String endDate,
                                       @QueryParam("branchId") Long branchId) {
        // TODO: Implement analytics reports
        ApiResponse<String> response = ApiResponse.success("Reportes de analytics obtenidos exitosamente");
        return Response.ok(response).build();
    }
    
    @GET
    @Path("/admin/transactions/export")
    @Operation(summary = "Export transactions", description = "Export transactions in various formats")
    public Response exportTransactions(@QueryParam("adminId") Long adminId,
                                      @QueryParam("format") @DefaultValue("csv") String format,
                                      @QueryParam("startDate") String startDate,
                                      @QueryParam("endDate") String endDate,
                                      @QueryParam("branchId") Long branchId) {
        // TODO: Implement transaction export
        ApiResponse<String> response = ApiResponse.success("Transacciones exportadas exitosamente");
        return Response.ok(response).build();
    }
}
