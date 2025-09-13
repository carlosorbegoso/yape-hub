package org.sky.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "yape_notifications")
public class YapeNotification extends PanacheEntity {
    
    @Column(name = "admin_id", nullable = false)
    public Long adminId;
    
    @Column(name = "encrypted_notification", columnDefinition = "TEXT", nullable = false)
    public String encryptedNotification;
    
    @Column(name = "device_fingerprint", nullable = false)
    public String deviceFingerprint;
    
    @Column(name = "timestamp", nullable = false)
    public Long timestamp;
    
    @Column(name = "transaction_id")
    public String transactionId;
    
    @Column(name = "amount")
    public Double amount;
    
    @Column(name = "sender_phone")
    public String senderPhone;
    
    @Column(name = "receiver_phone")
    public String receiverPhone;
    
    @Column(name = "status")
    public String status;
    
    @Column(name = "processed_at")
    public LocalDateTime processedAt;
    
    @Column(name = "is_processed", nullable = false)
    public Boolean isProcessed = false;
    
    @Column(name = "error_message")
    public String errorMessage;
    
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    public LocalDateTime updatedAt;
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
