package org.sky.dto.notification;

import org.sky.model.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(
    Long id,
    Notification.TargetType targetType,
    Long targetId,
    String title,
    String message,
    Notification.NotificationType type,
    String data,
    Boolean isRead,
    LocalDateTime readAt,
    LocalDateTime createdAt
) {}
