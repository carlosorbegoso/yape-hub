package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.Notification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class NotificationRepository implements PanacheRepository<Notification> {

    public Uni<List<Notification>> findByTargetTypeAndUserId(Notification.TargetType targetType, Long userId, 
                                                             Boolean unreadOnly, LocalDateTime startDate, LocalDateTime endDate) {
        if (unreadOnly != null && unreadOnly) {
            return find("targetType = ?1 and targetId = ?2 and isRead = false and createdAt >= ?3 and createdAt <= ?4 order by createdAt desc", 
                targetType, userId, startDate, endDate).list();
        } else {
            return find("targetType = ?1 and targetId = ?2 and createdAt >= ?3 and createdAt <= ?4 order by createdAt desc", 
                targetType, userId, startDate, endDate).list();
        }
    }

    public Uni<List<Notification>> findByUserIdForBothRoles(Long userId, Boolean unreadOnly, 
                                                           LocalDateTime startDate, LocalDateTime endDate) {
        Uni<List<Notification>> adminNotifications = findByTargetTypeAndUserId(
            Notification.TargetType.ADMIN, userId, unreadOnly, startDate, endDate);
        Uni<List<Notification>> sellerNotifications = findByTargetTypeAndUserId(
            Notification.TargetType.SELLER, userId, unreadOnly, startDate, endDate);

        return Uni.combine().all().unis(adminNotifications, sellerNotifications)
                .with((admin, seller) -> {
                    List<Notification> all = new ArrayList<>();
                    all.addAll(admin);
                    all.addAll(seller);
                    return all;
                });
    }
}