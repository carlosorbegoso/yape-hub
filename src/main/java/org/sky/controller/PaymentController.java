package org.sky.controller;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.sky.dto.ApiResponse;
import org.sky.dto.payment.PaymentClaimRequest;
import org.sky.dto.payment.PaymentClaimResponse;
import org.sky.dto.payment.PaymentRejectRequest;
import org.sky.annotation.TokenConsumption;
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
    @TokenConsumption(operationType = "payment_claim", tokens = 1)
    public Uni<Response> claimPayment(@Valid PaymentClaimRequest request,
                                     @HeaderParam("Authorization") String authorization) {
        log.info("🎯 PaymentController.claimPayment() - Vendedor reclamando pago");
        log.info("🎯 SellerId: " + request.sellerId());
        log.info("🎯 PaymentId: " + request.paymentId());
        
        // Validar autorización del vendedor
        return securityService.validateSellerAuthorization(authorization, request.sellerId())
                .chain(userId -> paymentNotificationService.claimPayment(request))
                .map(response -> Response.ok(ApiResponse.success("Pago reclamado exitosamente", response)).build())
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
        
        // Validar autorización del vendedor
        return securityService.validateSellerAuthorization(authorization, request.sellerId())
                .chain(userId -> paymentNotificationService.rejectPayment(request))
                .map(response -> Response.ok(ApiResponse.success("Pago rechazado exitosamente", response)).build())
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
               description = "Obtiene todos los pagos pendientes para un vendedor específico con paginación. Admins pueden ver pagos de sus vendedores.")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Pagos pendientes obtenidos exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado"),
        @APIResponse(responseCode = "404", description = "Vendedor no encontrado")
    })
    public Uni<Response> getPendingPayments(@QueryParam("sellerId") Long sellerId,
                                           @QueryParam("adminId") Long adminId,
                                           @QueryParam("page") @DefaultValue("0") int page,
                                           @QueryParam("size") @DefaultValue("20") int size,
                                           @QueryParam("limit") @DefaultValue("20") int limit,
                                           @HeaderParam("Authorization") String authorization) {
        log.info("📋 PaymentController.getPendingPayments() - Obteniendo pagos pendientes para vendedor: " + sellerId);
        log.info("📋 AdminId: " + adminId + ", Página: " + page + ", Tamaño: " + size + ", Limit: " + limit);
        
        // Usar limit como fallback si size no está especificado
        final int finalSize = (size == 20 && limit != 20) ? limit : size;
        
        // Validar token JWT primero
        return securityService.validateJwtToken(authorization)
                .chain(userId -> {
                    log.info("✅ Token válido para userId: " + userId);
                    
                    try {
                        // Si sellerId es null, solo permitir para ADMINs
                        if (sellerId == null) {
                            return securityService.validateAdminAuthorization(authorization, userId)
                                    .chain(adminUserId -> {
                                        log.info("✅ Usuario ADMIN autorizado para ver todos los pagos");
                                        return paymentNotificationService.getAllPendingPaymentsPaginated(page, finalSize);
                                    });
                        }
                        
                        // Si adminId está presente, validar que el admin puede acceder a este seller
                        if (adminId != null) {
                            return securityService.validateAdminCanAccessSeller(authorization, adminId, sellerId)
                                    .chain(adminUserId -> {
                                        log.info("✅ Admin " + adminId + " autorizado para ver pagos de seller " + sellerId);
                                        return paymentNotificationService.getPendingPaymentsForSellerPaginated(sellerId, page, finalSize);
                                    });
                        }
                        
                        // Si no hay adminId, validar autorización del vendedor directamente
                        return securityService.validateSellerAuthorization(authorization, sellerId)
                                .chain(sellerUserId -> {
                                    log.info("✅ Autorización exitosa para sellerId: " + sellerId);
                                    return paymentNotificationService.getPendingPaymentsForSellerPaginated(sellerId, page, finalSize);
                                });
                    } catch (Exception e) {
                        log.error("❌ Error en getPendingPayments: " + e.getMessage(), e);
                        return Uni.createFrom().failure(new RuntimeException("Error interno del servidor: " + e.getMessage()));
                    }
                })
                .map(pendingPaymentsResponse -> Response.ok(ApiResponse.success("Pagos pendientes obtenidos exitosamente", pendingPaymentsResponse)).build())
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
                .chain(userId -> paymentNotificationService.getAdminPaymentManagement(adminId, page, size, status))
                .map(managementResponse -> Response.ok(ApiResponse.success("Gestión de pagos obtenida exitosamente", managementResponse)).build())
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo gestión de pagos: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    @GET
    @Path("/notification-stats")
    @Operation(summary = "Get notification queue statistics", 
               description = "Obtiene estadísticas de la cola de notificaciones para debugging")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Estadísticas obtenidas exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado")
    })
    public Uni<Response> getNotificationStats(@HeaderParam("Authorization") String authorization) {
        log.info("📊 PaymentController.getNotificationStats() - Obteniendo estadísticas de notificaciones");
        
        // Validar token JWT (cualquier usuario autenticado puede ver estas stats)
        return securityService.validateJwtToken(authorization)
                .chain(userId -> {
                    Map<String, Object> stats = paymentNotificationService.getNotificationQueueStats();
                    return Uni.createFrom().item(stats);
                })
                .map(stats -> Response.ok(ApiResponse.success("Estadísticas de notificaciones obtenidas", stats)).build())
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo estadísticas: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    @GET
    @Path("/admin/connected-sellers")
    @Operation(summary = "Get connected sellers for admin", 
               description = "Obtiene información de vendedores conectados para un administrador específico")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Vendedores conectados obtenidos exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado"),
        @APIResponse(responseCode = "400", description = "Parámetros inválidos")
    })
    public Uni<Response> getConnectedSellersForAdmin(@QueryParam("adminId") Long adminId,
                                                     @HeaderParam("Authorization") String authorization) {
        log.info("📡 PaymentController.getConnectedSellersForAdmin() - AdminId: " + adminId);
        
        // Validar autorización de admin
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> paymentNotificationService.getConnectedSellersForAdmin(adminId))
                .map(connectedSellers -> {
                    // Crear respuesta con estadísticas
                    java.util.Map<String, Object> response = java.util.Map.of(
                        "adminId", adminId,
                        "connectedSellers", connectedSellers,
                        "totalConnected", connectedSellers.size(),
                        "timestamp", java.time.LocalDateTime.now()
                    );
                    
                    return Response.ok(ApiResponse.success("Vendedores conectados obtenidos exitosamente", response)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo vendedores conectados: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    @GET
    @Path("/admin/sellers-status")
    @Operation(summary = "Get all sellers status for admin", 
               description = "Obtiene el estado de conexión de todos los vendedores para un administrador específico")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Estado de vendedores obtenido exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado"),
        @APIResponse(responseCode = "400", description = "Parámetros inválidos")
    })
    public Uni<Response> getAllSellersStatusForAdmin(@QueryParam("adminId") Long adminId,
                                                     @HeaderParam("Authorization") String authorization) {
        log.info("📡 PaymentController.getAllSellersStatusForAdmin() - AdminId: " + adminId);
        
        // Validar autorización de admin
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> paymentNotificationService.getAllSellersStatusForAdmin(adminId))
                .map(allSellers -> {
                    // Contar conectados y desconectados
                    long connectedCount = allSellers.stream().filter(s -> s.isConnected).count();
                    long disconnectedCount = allSellers.size() - connectedCount;
                    
                    // Crear respuesta con estadísticas
                    java.util.Map<String, Object> response = java.util.Map.of(
                        "adminId", adminId,
                        "allSellers", allSellers,
                        "totalSellers", allSellers.size(),
                        "connectedCount", connectedCount,
                        "disconnectedCount", disconnectedCount,
                        "timestamp", java.time.LocalDateTime.now()
                    );
                    
                    return Response.ok(ApiResponse.success("Estado de vendedores obtenido exitosamente", response)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo estado de vendedores: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    @GET
    @Path("/confirmed")
    @Operation(summary = "Get confirmed payments for seller", 
               description = "Obtiene todos los pagos confirmados por un vendedor específico con paginación")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Pagos confirmados obtenidos exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado"),
        @APIResponse(responseCode = "404", description = "Vendedor no encontrado")
    })
    public Uni<Response> getConfirmedPayments(@QueryParam("sellerId") Long sellerId,
                                            @QueryParam("adminId") Long adminId,
                                            @QueryParam("page") @DefaultValue("0") int page,
                                            @QueryParam("size") @DefaultValue("20") int size,
                                            @HeaderParam("Authorization") String authorization) {
        log.info("✅ PaymentController.getConfirmedPayments() - Obteniendo pagos confirmados para vendedor: " + sellerId);
        log.info("✅ AdminId: " + adminId + ", Página: " + page + ", Tamaño: " + size);
        
        // Validar token JWT primero
        return securityService.validateJwtToken(authorization)
                .chain(userId -> {
                    log.info("✅ Token válido para userId: " + userId);
                    
                    // Si adminId está presente, validar que el admin puede acceder a este seller
                    if (adminId != null) {
                        return securityService.validateAdminCanAccessSeller(authorization, adminId, sellerId)
                                .chain(adminUserId -> {
                                    log.info("✅ Admin " + adminId + " autorizado para ver pagos confirmados de seller " + sellerId);
                                    return paymentNotificationService.getConfirmedPaymentsForSellerPaginated(sellerId, page, size);
                                });
                    }
                    
                    // Si no hay adminId, validar autorización del vendedor directamente
                    return securityService.validateSellerAuthorization(authorization, sellerId)
                            .chain(sellerUserId -> {
                                log.info("✅ Autorización exitosa para sellerId: " + sellerId);
                                return paymentNotificationService.getConfirmedPaymentsForSellerPaginated(sellerId, page, size);
                            });
                })
                .map(confirmedPaymentsResponse -> Response.ok(ApiResponse.success("Pagos confirmados obtenidos exitosamente", confirmedPaymentsResponse)).build())
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo pagos confirmados: " + throwable.getMessage());
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
