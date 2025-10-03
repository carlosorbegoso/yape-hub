package org.sky.service.notification.yape;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.ApiResponse;
import org.sky.dto.notification.NotificationResponse;
import org.sky.model.NotificationEntity;
import org.sky.repository.NotificationRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@ApplicationScoped
public class NotificationQueryService {

    @Inject
    NotificationRepository notificationRepository;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public Uni<ApiResponse<List<NotificationResponse>>> getNotifications(Long userId, String userRole,
                                                                         int page, int limit, Boolean unreadOnly, 
                                                                         String startDateStr, String endDateStr) {
        return parseDates(startDateStr, endDateStr)
                .chain(dates -> getNotificationsFromRepository(userRole, userId, unreadOnly, dates.startDate(), dates.endDate())
                        .map(notifications -> {
                            List<NotificationResponse> paginatedResponses = paginateAndConvert(notifications, page, limit);
                            return ApiResponse.success("Notifications retrieved successfully", paginatedResponses);
                        }));
    }
    
    private Uni<DateRange> parseDates(String startDateStr, String endDateStr) {
        try {
            LocalDate startDate;
          LocalDate endDate;
            if (startDateStr != null && endDateStr != null) {
                startDate = LocalDate.parse(startDateStr, DATE_FORMATTER);
                endDate = LocalDate.parse(endDateStr, DATE_FORMATTER);
            } else {
                endDate = LocalDate.now();
                startDate = endDate.minusDays(30);
            }
            return Uni.createFrom().item(new DateRange(startDate, endDate));
        } catch (DateTimeParseException e) {
            return Uni.createFrom().failure(
                new IllegalArgumentException("Invalid date format. Use yyyy-MM-dd")
            );
        }
    }
    
    private record DateRange(LocalDate startDate, LocalDate endDate) {}

    private Uni<List<NotificationEntity>> getNotificationsFromRepository(String userRole, Long userId, Boolean unreadOnly,
                                                                         LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        if ("ADMIN".equals(userRole)) {
            return notificationRepository.findByTargetTypeAndUserId(
                NotificationEntity.TargetType.ADMIN, userId, unreadOnly, startDateTime, endDateTime);
        } else if ("SELLER".equals(userRole)) {
            return notificationRepository.findByTargetTypeAndUserId(
                NotificationEntity.TargetType.SELLER, userId, unreadOnly, startDateTime, endDateTime);
        } else {
            return notificationRepository.findByUserIdForBothRoles(userId, unreadOnly, startDateTime, endDateTime);
        }
    }

    private List<NotificationResponse> paginateAndConvert(List<NotificationEntity> notifications, int page, int limit) {
        int totalItems = notifications.size();
        int startIndex = (page - 1) * limit;
        int endIndex = Math.min(startIndex + limit, totalItems);

        List<NotificationEntity> paginatedNotifications = notifications.subList(startIndex, endIndex);

        return paginatedNotifications.stream()
                .map(this::convertToResponse)
                .toList();
    }

    private NotificationResponse convertToResponse(NotificationEntity notification) {
        return new NotificationResponse(
            notification.id, notification.targetType, notification.targetId,
            notification.title, notification.message, notification.type,
            notification.data, notification.isRead, notification.readAt, notification.createdAt
        );
    }
}
