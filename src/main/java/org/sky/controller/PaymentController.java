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
import org.sky.service.PaymentNotificationService;
import org.sky.service.SecurityService;
import org.sky.service.WebSocketNotificationService;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import java.util.Map;

@Path("/api/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Payments", description = "Payment notification and SSE endpoints")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {
    
    @Inject
    PaymentNotificationService paymentNotificationService;

    @Inject
    SecurityService securityService;
    
    @Inject
    WebSocketNotificationService webSocketNotificationService;
    
    private static final Logger log = Logger.getLogger(PaymentController.class);
    
    
    
    @GET
    @Path("/status/{sellerId}")
    @Operation(summary = "Check seller connection status", description = "Check if a seller is connected via WebSocket")
    public Uni<Response> getSellerConnectionStatus(@PathParam("sellerId") Long sellerId,
                                                   @HeaderParam("Authorization") String authorization) {
        log.info("📡 PaymentController.getSellerConnectionStatus() - Verificando conexión para vendedor: " + sellerId);

        return securityService.validateSellerAuthorization(authorization, sellerId)
                .chain(userId -> {
                    boolean isConnected = webSocketNotificationService.isSellerConnected(sellerId);
                    int totalConnections = webSocketNotificationService.getConnectedSellersCount();
                    
                    Map<String, Object> status = Map.of(
                        "sellerId", sellerId,
                        "isConnected", isConnected,
                        "totalConnectedSellers", totalConnections,
                        "timestamp", java.time.LocalDateTime.now()
                    );
                    
                    return Uni.createFrom().item(Response.ok(ApiResponse.success("Estado de conexión obtenido", status)).build());
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error verificando estado de conexión: " + throwable.getMessage());
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
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    @POST
    @Path("/claim")
    @Operation(summary = "Claim payment", description = "Allow seller to claim a payment")
    public Uni<Response> claimPayment(@Valid PaymentClaimRequest request,
                                     @HeaderParam("Authorization") String authorization) {
        log.info("🎯 PaymentController.claimPayment() - Vendedor reclamando pago");
        log.info("🎯 SellerId: " + request.sellerId());
        log.info("🎯 PaymentId: " + request.paymentId());
        
        // Validar autorización del vendedor
        return securityService.validateSellerAuthorization(authorization, request.sellerId())
                .chain(userId -> {
                    log.info("✅ Autorización exitosa para sellerId: " + request.sellerId());
                    return paymentNotificationService.claimPayment(request);
                })
                .map(response -> {
                    log.info("✅ Pago reclamado exitosamente");
                    return Response.ok(ApiResponse.success("Pago reclamado exitosamente", response)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error reclamando pago: " + throwable.getMessage());
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
    
    @POST
    @Path("/reject")
    @Operation(summary = "Reject payment", description = "Allow seller to reject a payment")
    public Uni<Response> rejectPayment(@Valid PaymentRejectRequest request,
                                      @HeaderParam("Authorization") String authorization) {
        log.info("❌ PaymentController.rejectPayment() - Vendedor rechazando pago");
        log.info("❌ SellerId: " + request.sellerId());
        log.info("❌ PaymentId: " + request.paymentId());
        log.info("❌ Reason: " + request.reason());
        
        // Validar autorización del vendedor
        return securityService.validateSellerAuthorization(authorization, request.sellerId())
                .chain(userId -> {
                    log.info("✅ Autorización exitosa para sellerId: " + request.sellerId());
                    return paymentNotificationService.rejectPayment(request);
                })
                .map(response -> {
                    log.info("❌ Pago rechazado exitosamente");
                    return Response.ok(ApiResponse.success("Pago rechazado exitosamente", response)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error rechazando pago: " + throwable.getMessage());
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
    @Path("/pending")
    @Operation(summary = "Get pending payments for seller", 
               description = "Obtiene todos los pagos pendientes para un vendedor específico con paginación")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Pagos pendientes obtenidos exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado"),
        @APIResponse(responseCode = "404", description = "Vendedor no encontrado")
    })
    public Uni<Response> getPendingPayments(@QueryParam("sellerId") Long sellerId,
                                           @QueryParam("page") @DefaultValue("0") int page,
                                           @QueryParam("size") @DefaultValue("20") int size,
                                           @HeaderParam("Authorization") String authorization) {
        log.info("📋 PaymentController.getPendingPayments() - Obteniendo pagos pendientes para vendedor: " + sellerId);
        log.info("📋 Página: " + page + ", Tamaño: " + size);
        
        // Validar token JWT primero
        return securityService.validateJwtToken(authorization)
                .chain(userId -> {
                    log.info("✅ Token válido para userId: " + userId);
                    
                    // Si sellerId es null, solo permitir para ADMINs
                    if (sellerId == null) {
                        return securityService.validateAdminAuthorization(authorization, userId)
                                .chain(adminUserId -> {
                                    log.info("✅ Usuario ADMIN autorizado para ver todos los pagos");
                                    return paymentNotificationService.getAllPendingPaymentsPaginated(page, size);
                                });
                    }
                    
                    // Si sellerId está presente, validar autorización del vendedor
                    return securityService.validateSellerAuthorization(authorization, sellerId)
                            .chain(sellerUserId -> {
                                log.info("✅ Autorización exitosa para sellerId: " + sellerId);
                                return paymentNotificationService.getPendingPaymentsForSellerPaginated(sellerId, page, size);
                            });
                })
                .map(pendingPaymentsResponse -> {
                    log.info("✅ Pagos pendientes obtenidos: " + pendingPaymentsResponse.payments().size() + 
                           " (Página " + pendingPaymentsResponse.pagination().currentPage() + 
                           " de " + pendingPaymentsResponse.pagination().totalPages() + ")");
                    return Response.ok(ApiResponse.success("Pagos pendientes obtenidos exitosamente", pendingPaymentsResponse)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo pagos pendientes: " + throwable.getMessage());
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
    @Path("/admin/management")
    @Operation(summary = "Get admin payment management", 
               description = "Obtiene todos los pagos para gestión de administrador con información detallada")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Gestión de pagos obtenida exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado"),
        @APIResponse(responseCode = "400", description = "Parámetros inválidos")
    })
    public Uni<Response> getAdminPaymentManagement(@QueryParam("adminId") Long adminId,
                                                  @QueryParam("page") @DefaultValue("0") int page,
                                                  @QueryParam("size") @DefaultValue("20") int size,
                                                  @QueryParam("status") String status,
                                                  @HeaderParam("Authorization") String authorization) {
        log.info("👑 PaymentController.getAdminPaymentManagement() - AdminId: " + adminId);
        log.info("👑 Página: " + page + ", Tamaño: " + size + ", Status: " + status);
        
        // Validar autorización de admin
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> {
                    log.info("✅ Autorización exitosa para adminId: " + adminId);
                    return paymentNotificationService.getAdminPaymentManagement(adminId, page, size, status);
                })
                .map(managementResponse -> {
                    log.info("✅ Gestión de pagos obtenida exitosamente");
                    return Response.ok(ApiResponse.success("Gestión de pagos obtenida exitosamente", managementResponse)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo gestión de pagos: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
}
