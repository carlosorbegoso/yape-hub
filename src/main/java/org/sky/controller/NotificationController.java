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
import org.sky.service.notification.NotificationQueryService;
import org.sky.service.notification.NotificationUpdateService;
import org.sky.service.notification.YapeNotificationProcessor;
import org.sky.service.notification.YapeAuditService;
import org.sky.service.SecurityService;
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
    NotificationQueryService notificationQueryService;
    
    @Inject
    NotificationUpdateService notificationUpdateService;
    
    @Inject
    YapeNotificationProcessor yapeNotificationProcessor;
    
    @Inject
    YapeAuditService yapeAuditService;
    
    @Inject
    SecurityService securityService;
    
    @GET
    @Operation(summary = "Get notifications", description = "Get user notifications with pagination")
    public Uni<Response> getNotifications(@QueryParam("userId") Long userId,
                                    @QueryParam("userRole") String userRole,
                                    @QueryParam("startDate") String startDateStr,
                                    @QueryParam("endDate") String endDateStr,
                                    @QueryParam("page") @DefaultValue("1") int page,
                                    @QueryParam("limit") @DefaultValue("20") int limit,
                                    @QueryParam("unreadOnly") Boolean unreadOnly) {
        return notificationQueryService.getNotifications(userId, userRole, page, limit, unreadOnly, startDateStr, endDateStr)
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
        return notificationUpdateService.markNotificationAsRead(notificationId)
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
        return securityService.validateAdminAuthorization(authorization, request.adminId())
                .chain(userId -> yapeNotificationProcessor.processYapeNotification(request))
                .map(response -> {
                    if (response.isSuccess()) {
                        return Response.ok(response).build();
                    } else {
                        return Response.status(400).entity(response).build();
                    }
                })
                .onFailure().recoverWithItem(throwable -> securityService.handleSecurityException(throwable));
    }
    
    @GET
    @Path("/yape-audit")
    @Operation(summary = "Get Yape notification audit", description = "Get audit trail of Yape notifications for an admin")
    public Uni<Response> getYapeNotificationAudit(@QueryParam("adminId") Long adminId,
                                            @QueryParam("page") @DefaultValue("0") int page,
                                            @QueryParam("size") @DefaultValue("20") int size,
                                            @HeaderParam("Authorization") String authorization) {
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> yapeAuditService.getYapeNotificationAudit(adminId, page, size))
                .map(response -> {
                    if (response.isSuccess()) {
                        return Response.ok(response).build();
                    } else {
                        return Response.status(400).entity(response).build();
                    }
                })
                .onFailure().recoverWithItem(throwable -> securityService.handleSecurityException(throwable));
    }
}