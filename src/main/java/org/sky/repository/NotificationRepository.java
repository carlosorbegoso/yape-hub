package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.Notification;

import java.util.List;

@ApplicationScoped
public class NotificationRepository implements PanacheRepositoryBase<Notification, Long> {
    
    public Uni<List<Notification>> findByTargetTypeAndTargetId(Notification.TargetType targetType, Long targetId) {
        return find("targetType = ?1 and targetId = ?2", targetType, targetId).list();
    }
    
    public Uni<List<Notification>> findByTargetType(Notification.TargetType targetType) {
        return find("targetType", targetType).list();
    }
    
    public Uni<List<Notification>> findUnreadByTargetTypeAndTargetId(Notification.TargetType targetType, Long targetId) {
        return find("targetType = ?1 and targetId = ?2 and isRead = false", targetType, targetId).list();
    }
    
    public Uni<List<Notification>> findUnreadByTargetType(Notification.TargetType targetType) {
        return find("targetType = ?1 and isRead = false", targetType).list();
    }
    
    public Uni<List<Notification>> findByType(Notification.NotificationType type) {
        return find("type", type).list();
    }
    
    public Uni<List<Notification>> findUnreadNotifications() {
        return find("isRead = false").list();
    }
}
