package org.sky.dto.response.notification;

import org.sky.model.NotificationEntity;

import java.time.LocalDateTime;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record NotificationResponse(
    Long id,
    NotificationEntity.TargetType targetType,
    Long targetId,
    String title,
    String message,
    NotificationEntity.NotificationType type,
    String data,
    Boolean isRead,
    LocalDateTime readAt,
    LocalDateTime createdAt
) {
    // Constructor compacto - validaciones y normalizaciones
    public NotificationResponse {
        // Validaciones
        if (id == null) {
            throw new IllegalArgumentException("Notification ID cannot be null");
        }
        if (targetType == null) {
            throw new IllegalArgumentException("Target type cannot be null");
        }
        if (targetId == null) {
            throw new IllegalArgumentException("Target ID cannot be null");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }
        if (type == null) {
            throw new IllegalArgumentException("Notification type cannot be null");
        }
        
        // Normalizaciones
        title = title.trim();
        message = message.trim();
        data = data != null ? data.trim() : null;
        
        // Valores por defecto
        if (isRead == null) isRead = false;
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
    
    // Constructor desde NotificationEntity
    public static NotificationResponse fromEntity(NotificationEntity notification) {
        if (notification == null) {
            throw new IllegalArgumentException("Notification entity cannot be null");
        }
        
        return new NotificationResponse(
            notification.id,
            notification.targetType,
            notification.targetId,
            notification.title,
            notification.message,
            notification.type,
            notification.data,
            notification.isRead,
            notification.readAt,
            notification.createdAt
        );
    }
    
    // Constructor de conveniencia
    public static NotificationResponse create(NotificationEntity.TargetType targetType, Long targetId, 
                                            String title, String message, NotificationEntity.NotificationType type) {
        return new NotificationResponse(
            null, // ID se asignará automáticamente
            targetType,
            targetId,
            title,
            message,
            type,
            null,
            false,
            null,
            LocalDateTime.now()
        );
    }
}
