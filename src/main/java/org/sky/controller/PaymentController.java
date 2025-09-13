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
import org.sky.service.PaymentNotificationService;
import org.sky.service.SecurityService;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
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
    @Path("/sse/{sellerId}")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Operation(summary = "SSE for seller", description = "Server-Sent Events stream for payment notifications")
    public Multi<PaymentNotificationResponse> getPaymentNotifications(@PathParam("sellerId") Long sellerId,
                                                                    @HeaderParam("Authorization") String authorization) {
        log.info("📡 PaymentController.getPaymentNotifications() - Iniciando SSE para vendedor: " + sellerId);
        
        // Validar autorización del vendedor
        return Multi.createFrom().emitter(emitter -> {
            // Crear un emisor SSE para este vendedor
            Multi<PaymentNotificationResponse> sellerMulti = Multi.createFrom().emitter(sellerEmitter -> {
                // Registrar la conexión
                PaymentNotificationResponse connection = new PaymentNotificationResponse(
                    null, null, null, null, "CONNECTED", 
                    java.time.LocalDateTime.now(), "Conexión SSE establecida"
                );
                paymentNotificationService.registerConnection(sellerId, connection);
                
                // Mantener la conexión activa
                sellerEmitter.onTermination(() -> {
                    paymentNotificationService.unregisterConnection(sellerId, connection);
                    log.info("🔌 SSE desconectado para vendedor: " + sellerId);
                });
                
                // Enviar heartbeat cada 30 segundos
                sellerEmitter.emit(connection);
            });
            
        // Almacenar el emisor para este vendedor
        sseEmitters.put(sellerId, sellerMulti);
        
        // Emitir el stream
        emitter.emit(sellerMulti);
    })
    .onFailure().recoverWithItem(throwable -> {
        log.error("❌ Error en SSE para vendedor " + sellerId + ": " + throwable.getMessage());
        return new PaymentNotificationResponse(
            null, null, null, null, "ERROR",
            java.time.LocalDateTime.now(), "Error en conexión SSE: " + throwable.getMessage()
        );
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
}
