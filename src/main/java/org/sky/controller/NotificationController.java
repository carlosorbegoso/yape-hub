package org.sky.controller;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.sky.dto.ApiResponse;
import org.sky.dto.notification.NotificationResponse;
import org.sky.dto.notification.SendNotificationRequest;
import org.sky.service.NotificationService;
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
    
    @POST
    @Path("/send")
    @Operation(summary = "Send notification", description = "Send push notification to users")
    public Uni<Response> sendNotification(@Valid SendNotificationRequest request) {
        return notificationService.sendNotification(request)
                .map(response -> {
                    if (response.isSuccess()) {
                        return Response.ok(response).build();
                    } else {
                        return Response.status(400).entity(response).build();
                    }
                });
    }
    
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
}
