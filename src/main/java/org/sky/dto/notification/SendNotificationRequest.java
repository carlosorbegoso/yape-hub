package org.sky.dto.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.sky.model.Notification;

public record SendNotificationRequest(
    @NotNull(message = "Target type is required")
    Notification.TargetType targetType,
    
    Long targetId,
    
    @NotBlank(message = "Title is required")
    String title,
    
    @NotBlank(message = "Message is required")
    String message,
    
    @NotNull(message = "Type is required")
    Notification.NotificationType type,
    
    String data
) {}
