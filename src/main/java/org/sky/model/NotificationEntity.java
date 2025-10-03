package org.sky.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class NotificationEntity extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    public TargetType targetType;
    
    @Column(name = "target_id")
    public Long targetId;
    
    @NotBlank
    @Column(nullable = false)
    public String title;
    
    @NotBlank
    @Column(columnDefinition = "TEXT", nullable = false)
    public String message;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public NotificationType type;
    
    @Column(columnDefinition = "TEXT")
    public String data;
    
    @Column(name = "is_read")
    public Boolean isRead = false;
    
    @Column(name = "read_at")
    public LocalDateTime readAt;
    
    @CreationTimestamp
    @Column(name = "created_at")
    public LocalDateTime createdAt;
    
    public enum TargetType {
        ADMIN, SELLER, ALL
    }
    
    public enum NotificationType {
        TRANSACTION, SYSTEM, PROMOTION, SECURITY
    }
    
    // Constructors
    public NotificationEntity() {}
    
    public NotificationEntity(TargetType targetType, Long targetId, String title, String message, NotificationType type) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.title = title;
        this.message = message;
        this.type = type;
    }
    
}
