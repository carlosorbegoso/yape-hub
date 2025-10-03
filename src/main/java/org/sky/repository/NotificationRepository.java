package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.NotificationEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class NotificationRepository implements PanacheRepository<NotificationEntity> {

    public Uni<List<NotificationEntity>> findByTargetTypeAndUserId(NotificationEntity.TargetType targetType, Long userId,
                                                                   Boolean unreadOnly, LocalDateTime startDate, LocalDateTime endDate) {
        if (unreadOnly != null && unreadOnly) {
            return find("targetType = ?1 and targetId = ?2 and isRead = false and createdAt >= ?3 and createdAt <= ?4 order by createdAt desc", 
                targetType, userId, startDate, endDate).list();
        } else {
            return find("targetType = ?1 and targetId = ?2 and createdAt >= ?3 and createdAt <= ?4 order by createdAt desc", 
                targetType, userId, startDate, endDate).list();
        }
    }

    public Uni<List<NotificationEntity>> findByUserIdForBothRoles(Long userId, Boolean unreadOnly,
                                                                  LocalDateTime startDate, LocalDateTime endDate) {
        Uni<List<NotificationEntity>> adminNotifications = findByTargetTypeAndUserId(
            NotificationEntity.TargetType.ADMIN, userId, unreadOnly, startDate, endDate);
        Uni<List<NotificationEntity>> sellerNotifications = findByTargetTypeAndUserId(
            NotificationEntity.TargetType.SELLER, userId, unreadOnly, startDate, endDate);

        return Uni.combine().all().unis(adminNotifications, sellerNotifications)
                .with((admin, seller) -> {
                    List<NotificationEntity> all = new ArrayList<>();
                    all.addAll(admin);
                    all.addAll(seller);
                    return all;
                });
    }
}