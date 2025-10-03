package org.sky.service.notification.yape;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import org.sky.dto.response.ApiResponse;
import org.sky.repository.NotificationRepository;

import java.time.LocalDateTime;

@ApplicationScoped
public class NotificationUpdateService {

    @Inject
    NotificationRepository notificationRepository;

    @WithTransaction
    public Uni<ApiResponse<String>> markNotificationAsRead(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .chain(notification -> {
                    if (notification == null) {
                        return Uni.createFrom().item(ApiResponse.<String>error("Notification not found"));
                    }

                    notification.isRead = true;
                    notification.readAt = LocalDateTime.now();

                    return notificationRepository.persist(notification)
                            .map(persistedNotification -> ApiResponse.success("Notification marked as read"));
                });
    }
}
