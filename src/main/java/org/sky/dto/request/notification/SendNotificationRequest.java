package org.sky.dto.request.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.sky.model.NotificationEntity;

public record SendNotificationRequest(
    @NotNull(message = "Target type is required")
    NotificationEntity.TargetType targetType,
    
    Long targetId,
    
    @NotBlank(message = "Title is required")
    String title,
    
    @NotBlank(message = "Message is required")
    String message,
    
    @NotNull(message = "Type is required")
    NotificationEntity.NotificationType type,
    
    String data
) {}
