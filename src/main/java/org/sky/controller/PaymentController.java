package org.sky.controller;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.sky.dto.request.payment.PaymentClaimRequest;

import org.sky.dto.request.payment.PaymentRejectRequest;
import org.sky.dto.response.ApiResponse;
import org.sky.dto.response.ErrorResponse;
import java.util.Map;
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
    @Path("/")
    @Operation(summary = "Get payments based on user role and status", 
               description = "Get payments filtered by user role and status: Admin sees all payments from their sellers, Seller sees only their own payments. Status can be PENDING, CLAIMED, REJECTED, or ALL (ALL only available for ADMIN role). Multiple statuses can be combined with commas (e.g., PENDING,CLAIMED).")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Payments retrieved successfully"),
        @APIResponse(responseCode = "401", description = "Unauthorized"),
        @APIResponse(responseCode = "403", description = "Forbidden - insufficient permissions (e.g., Seller trying to access ALL status)"),
        @APIResponse(responseCode = "400", description = "Invalid parameters")
    })
    public Uni<Response> getPayments(@QueryParam("sellerId") Long sellerId,
                                    @QueryParam("status") @DefaultValue("ALL") String status,
                                    @QueryParam("startDate") String startDateStr,
                                    @QueryParam("endDate") String endDateStr,
                                    @QueryParam("page") @DefaultValue("0") int page,
                                    @QueryParam("size") @DefaultValue("20") int size,
                                    @QueryParam("limit") @DefaultValue("20") int limit,
                                    @HeaderParam("Authorization") String authorization) {
        return securityService.validateJwtToken(authorization)
                .chain(userId -> {
                    // Determinar el rol del usuario y obtener los pagos apropiados
                    return hubNotificationControllerService.getPaymentsByRoleAndStatus(userId, sellerId, status, startDateStr, endDateStr, page, size, limit);
                })
                .map(paymentsResponse -> Response.ok(ApiResponse.success("Pagos obtenidos exitosamente", paymentsResponse)).build())
                .onFailure().recoverWithItem(throwable -> {
                    if (throwable instanceof IllegalArgumentException) {
                        return Response.status(400).entity(ErrorResponse.validationError(throwable.getMessage(), Map.of())).build();
                    }
                    if (throwable instanceof SecurityException) {
                        return Response.status(403).entity(ErrorResponse.create("Acceso denegado: " + throwable.getMessage(), "FORBIDDEN")).build();
                    }
                    return ControllerErrorHandler.handleControllerError(throwable);
                });
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
    
}
