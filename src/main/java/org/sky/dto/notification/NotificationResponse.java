package org.sky.dto.notification;

import org.sky.model.NotificationEntity;

import java.time.LocalDateTime;

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
) {}
