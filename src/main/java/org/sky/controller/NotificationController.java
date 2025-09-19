package org.sky.controller;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.sky.dto.ApiResponse;
import org.sky.dto.notification.NotificationResponse;
import org.sky.dto.notification.YapeNotificationRequest;
import org.sky.dto.notification.YapeNotificationResponse;
import org.sky.dto.notification.YapeAuditResponse;
import org.sky.service.NotificationService;
import org.sky.service.SecurityService;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;

import java.util.List;

@Path("/api/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Notifications", description = "Notification system endpoints")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {
    
    @Inject
    NotificationService notificationService;
    
    @Inject
    SecurityService securityService;
    
    private static final Logger log = Logger.getLogger(NotificationController.class);
    
    
    @GET
    @Operation(summary = "Get notifications", description = "Get user notifications with pagination")
    public Uni<Response> getNotifications(@QueryParam("userId") Long userId,
                                    @QueryParam("userRole") String userRole,
                                    @QueryParam("page") @DefaultValue("1") int page,
                                    @QueryParam("limit") @DefaultValue("20") int limit,
                                    @QueryParam("unreadOnly") Boolean unreadOnly) {
        return notificationService.getNotifications(userId, userRole, page, limit, unreadOnly)
                .map(response -> {
                    if (response.isSuccess()) {
                        return Response.ok(response).build();
                    } else {
                        return Response.status(400).entity(response).build();
                    }
                });
    }
    
    @POST
    @Path("/{notificationId}/read")
    @Operation(summary = "Mark notification as read", description = "Mark a notification as read")
    public Uni<Response> markNotificationAsRead(@PathParam("notificationId") Long notificationId) {
        return notificationService.markNotificationAsRead(notificationId)
                .map(response -> {
                    if (response.isSuccess()) {
                        return Response.ok(response).build();
                    } else {
                        return Response.status(400).entity(response).build();
                    }
                });
    }
    
    @POST
    @Path("/yape-notifications")
    @Operation(summary = "Process Yape notification", description = "Process encrypted Yape notification for transactions")
    public Uni<Response> processYapeNotification(@Valid YapeNotificationRequest request,
                                                @HeaderParam("Authorization") String authorization) {
        log.info("🚀 NotificationController.processYapeNotification() - Procesando notificación de Yape para adminId: " + request.adminId());
        log.info("🚀 Device fingerprint: " + request.deviceFingerprint());
        log.info("🚀 Timestamp: " + request.timestamp());
        
        // Validar autorización de admin
        return securityService.validateAdminAuthorization(authorization, request.adminId())
                .chain(userId -> {
                    log.info("✅ Autorización exitosa para adminId: " + request.adminId());
                    return notificationService.processYapeNotification(request);
                })
                .map(response -> {
                    if (response.isSuccess()) {
                        log.info("✅ Notificación de Yape procesada exitosamente");
                        return Response.ok(response).build();
                    } else {
                        log.warn("⚠️ Error al procesar notificación de Yape: " + response.message());
                        return Response.status(400).entity(response).build();
                    }
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error en procesamiento de notificación: " + throwable.getMessage());
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
    @Path("/yape-audit")
    @Operation(summary = "Get Yape notification audit", description = "Get audit trail of Yape notifications for an admin")
    public Uni<Response> getYapeNotificationAudit(@QueryParam("adminId") Long adminId,
                                            @QueryParam("page") @DefaultValue("0") int page,
                                            @QueryParam("size") @DefaultValue("20") int size,
                                            @HeaderParam("Authorization") String authorization) {
        log.info("📋 NotificationController.getYapeNotificationAudit() - AdminId: " + adminId);
        
        // Validar autorización de admin
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> {
                    log.info("✅ Autorización exitosa para auditoría de adminId: " + adminId);
                    return notificationService.getYapeNotificationAudit(adminId, page, size);
                })
                .map(response -> {
                    if (response.isSuccess()) {
                        log.info("✅ Auditoría de Yape obtenida exitosamente");
                        return Response.ok(response).build();
                    } else {
                        log.warn("⚠️ Error al obtener auditoría: " + response.message());
                        return Response.status(400).entity(response).build();
                    }
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error en auditoría: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
}
