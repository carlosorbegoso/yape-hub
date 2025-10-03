package org.sky.controller;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.sky.dto.ApiResponse;
import org.sky.dto.payment.PaymentClaimRequest;
import org.sky.dto.payment.PaymentRejectRequest;
import org.sky.service.hubnotifications.HubNotificationControllerService;
import org.sky.service.security.SecurityService;
import org.sky.util.ControllerErrorHandler;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

@Path("/api/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Payments", description = "Payment notification and SSE endpoints")
@jakarta.annotation.security.PermitAll
public class PaymentController {
    
    @Inject
    HubNotificationControllerService hubNotificationControllerService;
    
    @Inject
    SecurityService securityService;
    
    @GET
    @Path("/status/{sellerId}")
    @Operation(summary = "Check seller connection status", description = "Check if a seller is connected via WebSocket")
    public Uni<Response> getSellerConnectionStatus(@PathParam("sellerId") Long sellerId,
                                                   @HeaderParam("Authorization") String authorization) {
        return securityService.validateJwtToken(authorization)
                .chain(adminId -> hubNotificationControllerService.getSellerConnectionStatus(sellerId, adminId))
                .map(status -> Response.ok(ApiResponse.success("Connection status retrieved", status)).build())
                .onFailure().recoverWithItem(ControllerErrorHandler::handleControllerError);
    }
    
    @POST
    @Path("/claim")
    @Operation(summary = "Claim payment", description = "Allow seller to claim a payment")
    public Uni<Response> claimPayment(@Valid PaymentClaimRequest request,
                                     @HeaderParam("Authorization") String authorization) {
        return securityService.validateJwtToken(authorization)
                .chain(userId -> hubNotificationControllerService.claimPayment(request, userId, null))
                .map(response -> Response.ok(ApiResponse.success("Payment claimed successfully", response)).build())
                .onFailure().recoverWithItem(ControllerErrorHandler::handleControllerError);
    }
    
    @POST
    @Path("/reject")
    @Operation(summary = "Reject payment", description = "Allow seller to reject a payment")
    public Uni<Response> rejectPayment(@Valid PaymentRejectRequest request,
                                      @HeaderParam("Authorization") String authorization) {
        return securityService.validateJwtToken(authorization)
                .chain(userId -> hubNotificationControllerService.rejectPayment(request, userId, null))
                .map(response -> Response.ok(ApiResponse.success("Payment rejected successfully", response)).build())
                .onFailure().recoverWithItem(ControllerErrorHandler::handleControllerError);
    }
    
    @GET
    @Path("/pending")
    @Operation(summary = "Get pending payments for seller", 
               description = "Get all pending payments for a specific seller with pagination. Admins can view payments from their sellers.")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Pending payments retrieved successfully"),
        @APIResponse(responseCode = "401", description = "Unauthorized"),
        @APIResponse(responseCode = "404", description = "Seller not found")
    })
    public Uni<Response> getPendingPayments(@QueryParam("sellerId") Long sellerId,
                                           @QueryParam("adminId") Long adminId,
                                           @QueryParam("startDate") String startDateStr,
                                           @QueryParam("endDate") String endDateStr,
                                           @QueryParam("page") @DefaultValue("0") int page,
                                           @QueryParam("size") @DefaultValue("20") int size,
                                           @QueryParam("limit") @DefaultValue("20") int limit,
                                           @HeaderParam("Authorization") String authorization) {
        return securityService.validateJwtToken(authorization)
                .chain(userId -> hubNotificationControllerService.getPendingPayments(sellerId, adminId, startDateStr, endDateStr, page, size, limit))
                .map(pendingPaymentsResponse -> Response.ok(ApiResponse.success("Pending payments retrieved successfully", pendingPaymentsResponse)).build())
                .onFailure().recoverWithItem(throwable -> {
                    if (throwable instanceof IllegalArgumentException) {
                        return Response.status(400).entity(ApiResponse.error(throwable.getMessage())).build();
                    }
                    return ControllerErrorHandler.handleControllerError(throwable);
                });
    }
    
    @GET
    @Path("/admin/management")
    @Operation(summary = "Get admin payment management", 
               description = "Get all payments for admin management with detailed information")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Payment management retrieved successfully"),
        @APIResponse(responseCode = "401", description = "Unauthorized"),
        @APIResponse(responseCode = "400", description = "Invalid parameters")
    })
    public Uni<Response> getAdminPaymentManagement(@QueryParam("adminId") Long adminId,
                                                  @QueryParam("startDate") String startDateStr,
                                                  @QueryParam("endDate") String endDateStr,
                                                  @QueryParam("page") @DefaultValue("0") int page,
                                                  @QueryParam("size") @DefaultValue("20") int size,
                                                  @QueryParam("status") String status,
                                                  @HeaderParam("Authorization") String authorization) {
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> hubNotificationControllerService.getAdminPaymentManagement(adminId, page, size, status, startDateStr, endDateStr))
                .map(managementResponse -> Response.ok(ApiResponse.success("Payment management retrieved successfully", managementResponse)).build())
                .onFailure().recoverWithItem(throwable -> {
                    if (throwable instanceof IllegalArgumentException) {
                        return Response.status(400).entity(ApiResponse.error(throwable.getMessage())).build();
                    }
                    return ControllerErrorHandler.handleControllerError(throwable);
                });
    }
    
    @GET
    @Path("/notification-stats")
    @Operation(summary = "Get notification queue statistics", 
               description = "Get notification queue statistics for debugging")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @APIResponse(responseCode = "401", description = "Unauthorized")
    })
    public Uni<Response> getNotificationStats(@HeaderParam("Authorization") String authorization) {
        return securityService.validateJwtToken(authorization)
                .chain(userId -> {
                    java.util.Map<String, Object> stats = java.util.Map.of(
                        "message", "Queue stats retrieved successfully",
                        "processedCount", 0, // Simplified
                        "timestamp", java.time.LocalDateTime.now()
                    );
                    return Uni.createFrom().item(stats);
                })
                .map(stats -> Response.ok(ApiResponse.success("Notification statistics retrieved", stats)).build())
                .onFailure().recoverWithItem(ControllerErrorHandler::handleControllerError);
    }
    
    @GET
    @Path("/admin/connected-sellers")
    @Operation(summary = "Get connected sellers for admin", 
               description = "Get connected sellers information for a specific administrator")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Connected sellers retrieved successfully"),
        @APIResponse(responseCode = "401", description = "Unauthorized"),
        @APIResponse(responseCode = "400", description = "Invalid parameters")
    })
    public Uni<Response> getConnectedSellersForAdmin(@QueryParam("adminId") Long adminId,
                                                     @HeaderParam("Authorization") String authorization) {
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> hubNotificationControllerService.getConnectedSellersForAdmin(adminId))
                .map(connectedSellers -> {
                    java.util.Map<String, Object> response = java.util.Map.of(
                        "adminId", adminId,
                        "connectedSellers", connectedSellers,
                        "totalConnected", connectedSellers.size(),
                        "timestamp", java.time.LocalDateTime.now()
                    );
                    
                    return Response.ok(ApiResponse.success("Connected sellers retrieved successfully", response)).build();
                })
                .onFailure().recoverWithItem(ControllerErrorHandler::handleControllerError);
    }
    
    @GET
    @Path("/admin/sellers-status")
    @Operation(summary = "Get all sellers status for admin", 
               description = "Get connection status of all sellers for a specific administrator")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Sellers status retrieved successfully"),
        @APIResponse(responseCode = "401", description = "Unauthorized"),
        @APIResponse(responseCode = "400", description = "Invalid parameters")
    })
    public Uni<Response> getAllSellersStatusForAdmin(@QueryParam("adminId") Long adminId,
                                                     @HeaderParam("Authorization") String authorization) {
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> hubNotificationControllerService.getAllSellersStatusForAdmin(adminId))
                .map(response -> Response.ok(ApiResponse.success("Sellers status retrieved successfully", response)).build())
                .onFailure().recoverWithItem(ControllerErrorHandler::handleControllerError);
    }
    
    @GET
    @Path("/confirmed")
    @Operation(summary = "Get confirmed payments for seller", 
               description = "Get all confirmed payments by a specific seller with pagination")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Confirmed payments retrieved successfully"),
        @APIResponse(responseCode = "401", description = "Unauthorized"),
        @APIResponse(responseCode = "404", description = "Seller not found")
    })
    public Uni<Response> getConfirmedPayments(@QueryParam("sellerId") Long sellerId,
                                            @QueryParam("adminId") Long adminId,
                                            @QueryParam("startDate") String startDateStr,
                                            @QueryParam("endDate") String endDateStr,
                                            @QueryParam("page") @DefaultValue("0") int page,
                                            @QueryParam("size") @DefaultValue("20") int size,
                                            @HeaderParam("Authorization") String authorization) {
        return securityService.validateJwtToken(authorization)
                .chain(userId -> hubNotificationControllerService.getConfirmedPaymentsForSeller(sellerId, userId, startDateStr, endDateStr))
                .map(confirmedPaymentsResponse -> Response.ok(ApiResponse.success("Confirmed payments retrieved successfully", confirmedPaymentsResponse)).build())
                .onFailure().recoverWithItem(throwable -> {
                    if (throwable instanceof IllegalArgumentException) {
                        return Response.status(400).entity(ApiResponse.error(throwable.getMessage())).build();
                    }
                    return ControllerErrorHandler.handleControllerError(throwable);
                });
    }
}
