package org.sky.controller;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.sky.dto.ApiResponse;
import org.sky.dto.payment.PaymentNotificationRequest;
import org.sky.dto.payment.PaymentNotificationResponse;
import org.sky.dto.payment.PaymentClaimRequest;
import org.sky.dto.payment.PendingPaymentsResponse;
import org.sky.service.PaymentNotificationService;
import org.sky.service.SecurityService;
import org.sky.service.WebSocketNotificationService;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    
    // Map para almacenar los emisores SSE por sellerId
    private final Map<Long, Multi<PaymentNotificationResponse>> sseEmitters = new ConcurrentHashMap<>();
    
    @POST
    @Path("/notify")
    @Operation(summary = "Notify payment", description = "Process payment notification and broadcast to sellers")
    public Uni<Response> notifyPayment(@Valid PaymentNotificationRequest request,
                                     @HeaderParam("Authorization") String authorization) {
        log.info("💰 PaymentController.notifyPayment() - Procesando notificación de pago");
        log.info("💰 AdminId: " + request.adminId());
        log.info("💰 Monto: " + request.amount());
        log.info("💰 Remitente: " + request.senderName());
        log.info("💰 Código: " + request.yapeCode());
        
        // Validar autorización de admin
        return securityService.validateAdminAuthorization(authorization, request.adminId())
                .chain(userId -> {
                    log.info("✅ Autorización exitosa para adminId: " + request.adminId());
                    return paymentNotificationService.processPaymentNotification(request);
                })
                .map(response -> {
                    log.info("✅ Notificación de pago procesada exitosamente");
                    return Response.ok(ApiResponse.success("Notificación de pago enviada a vendedores", response)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error en notificación de pago: " + throwable.getMessage());
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
        return securityService.validateJwtToken(authorization)
                .chain(userId -> {
                    if (!userId.equals(request.sellerId())) {
                        return Uni.createFrom().failure(new SecurityException("No autorizado para este vendedor"));
                    }
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
    
    /**
     * Endpoint de prueba para enviar notificación directa via WebSocket
     */
    @POST
    @Path("/test-notification/{sellerId}")
    @Operation(summary = "Enviar notificación de prueba via WebSocket", 
               description = "Envía una notificación de prueba directamente a un vendedor específico")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Notificación enviada exitosamente"),
        @APIResponse(responseCode = "404", description = "Vendedor no encontrado"),
        @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Response testNotification(@PathParam("sellerId") Long sellerId) {
        try {
            log.info("🧪 Enviando notificación de prueba a vendedor: " + sellerId);
            
            // Crear notificación de prueba
            PaymentNotificationResponse testNotification = new PaymentNotificationResponse(
                999L, // paymentId de prueba
                50.0, // amount
                "Test Sender", // senderName
                "TEST123", // yapeCode
                "PENDING", // status
                LocalDateTime.now(), // timestamp
                "Notificación de prueba - ¿Es tu cliente?" // message
            );
            
            // Enviar via WebSocket
            paymentNotificationService.sendToSellerDirectly(sellerId, testNotification);
            
            return Response.ok(ApiResponse.success("Notificación de prueba enviada", 
                Map.of("sellerId", sellerId, "message", "Notificación enviada via WebSocket"))).build();
                
        } catch (Exception e) {
            log.error("❌ Error enviando notificación de prueba: " + e.getMessage());
            return Response.status(500).entity(ApiResponse.error("Error enviando notificación de prueba: " + e.getMessage())).build();
        }
    }
    
    @GET
    @Path("/pending/{sellerId}")
    @Operation(summary = "Get pending payments for seller", 
               description = "Obtiene todos los pagos pendientes para un vendedor específico con paginación")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Pagos pendientes obtenidos exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado"),
        @APIResponse(responseCode = "404", description = "Vendedor no encontrado")
    })
    public Uni<Response> getPendingPayments(@PathParam("sellerId") Long sellerId,
                                           @QueryParam("page") @DefaultValue("0") int page,
                                           @QueryParam("size") @DefaultValue("20") int size,
                                           @HeaderParam("Authorization") String authorization) {
        log.info("📋 PaymentController.getPendingPayments() - Obteniendo pagos pendientes para vendedor: " + sellerId);
        log.info("📋 Página: " + page + ", Tamaño: " + size);
        
        // Validar autorización del vendedor
        return securityService.validateSellerAuthorization(authorization, sellerId)
                .chain(userId -> {
                    log.info("✅ Autorización exitosa para sellerId: " + sellerId);
                    return paymentNotificationService.getPendingPaymentsForSellerPaginated(sellerId, page, size);
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
}
