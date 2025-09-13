package org.sky.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import org.sky.dto.ApiResponse;
import org.sky.dto.notification.NotificationResponse;
import org.sky.dto.notification.SendNotificationRequest;
import org.sky.model.Notification;
import org.sky.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class NotificationService {
    
    @Inject
    NotificationRepository notificationRepository;
    
    @WithTransaction
    public Uni<ApiResponse<String>> sendNotification(SendNotificationRequest request) {
        Notification notification = new Notification();
        notification.targetType = request.targetType();
        notification.targetId = request.targetId();
        notification.title = request.title();
        notification.message = request.message();
        notification.type = request.type();
        notification.data = request.data();
        
        return notificationRepository.persist(notification)
                .map(persistedNotification -> {
                    // TODO: Send push notification to mobile devices
                    // TODO: Send email notification if configured
                    return ApiResponse.success("Notificación enviada exitosamente");
                });
    }
    
    public Uni<ApiResponse<List<NotificationResponse>>> getNotifications(Long userId, String userRole, 
                                                                  int page, int limit, Boolean unreadOnly) {
        Uni<List<Notification>> notificationsUni;
        
        if ("ADMIN".equals(userRole)) {
            if (unreadOnly != null && unreadOnly) {
                notificationsUni = notificationRepository.findUnreadByTargetTypeAndTargetId(
                        Notification.TargetType.ADMIN, userId);
            } else {
                notificationsUni = notificationRepository.findByTargetTypeAndTargetId(
                        Notification.TargetType.ADMIN, userId);
            }
        } else if ("SELLER".equals(userRole)) {
            if (unreadOnly != null && unreadOnly) {
                notificationsUni = notificationRepository.findUnreadByTargetTypeAndTargetId(
                        Notification.TargetType.SELLER, userId);
            } else {
                notificationsUni = notificationRepository.findByTargetTypeAndTargetId(
                        Notification.TargetType.SELLER, userId);
            }
        } else {
            // Get all notifications for user
            Uni<List<Notification>> adminNotificationsUni = notificationRepository.findByTargetTypeAndTargetId(
                    Notification.TargetType.ADMIN, userId);
            Uni<List<Notification>> sellerNotificationsUni = notificationRepository.findByTargetTypeAndTargetId(
                    Notification.TargetType.SELLER, userId);
            
            notificationsUni = Uni.combine().all().unis(adminNotificationsUni, sellerNotificationsUni)
                    .with((adminNotifications, sellerNotifications) -> {
                        List<Notification> allNotifications = new java.util.ArrayList<>();
                        allNotifications.addAll(adminNotifications);
                        allNotifications.addAll(sellerNotifications);
                        return allNotifications;
                    });
        }
        
        return notificationsUni.map(notifications -> {
            // Pagination
            int totalItems = notifications.size();
            int startIndex = (page - 1) * limit;
            int endIndex = Math.min(startIndex + limit, totalItems);
            
            List<Notification> paginatedNotifications = notifications.subList(startIndex, endIndex);
            
            List<NotificationResponse> responses = paginatedNotifications.stream()
                    .map(notification -> new NotificationResponse(
                            notification.id, notification.targetType, notification.targetId,
                            notification.title, notification.message, notification.type,
                            notification.data, notification.isRead, notification.readAt, notification.createdAt
                    ))
                    .collect(Collectors.toList());
            
            return ApiResponse.success("Notificaciones obtenidas exitosamente", responses);
        });
    }
    
    @WithTransaction
    public Uni<ApiResponse<String>> markNotificationAsRead(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .chain(notification -> {
                    if (notification == null) {
                        return Uni.createFrom().item(ApiResponse.<String>error("Notificación no encontrada"));
                    }
                    
                    notification.isRead = true;
                    notification.readAt = LocalDateTime.now();
                    
                    return notificationRepository.persist(notification)
                            .map(persistedNotification -> ApiResponse.success("Notificación marcada como leída"));
                });
    }
}
