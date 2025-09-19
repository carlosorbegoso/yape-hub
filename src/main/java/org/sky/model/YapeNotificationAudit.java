package org.sky.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "yape_notification_audit")
public class YapeNotificationAudit extends PanacheEntity {
    
    @Column(name = "admin_id", nullable = false)
    public Long adminId;
    
    @Column(name = "encrypted_notification", columnDefinition = "TEXT", nullable = false)
    public String encryptedNotification;
    
    @Column(name = "device_fingerprint", nullable = false)
    public String deviceFingerprint;
    
    @Column(name = "timestamp", nullable = false)
    public Long timestamp;
    
    @Column(name = "deduplication_hash", nullable = false, unique = true)
    public String deduplicationHash;
    
    @Column(name = "decryption_status", nullable = false)
    public String decryptionStatus = "PENDING"; // PENDING, SUCCESS, FAILED
    
    @Column(name = "decryption_error", columnDefinition = "TEXT")
    public String decryptionError;
    
    @Column(name = "extracted_amount")
    public Double extractedAmount;
    
    @Column(name = "extracted_sender_name")
    public String extractedSenderName;
    
    @Column(name = "extracted_yape_code")
    public String extractedYapeCode;
    
    @Column(name = "transaction_id")
    public String transactionId;
    
    @Column(name = "payment_notification_id")
    public Long paymentNotificationId;
    
    @CreationTimestamp
    @Column(name = "created_at")
    public LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    public LocalDateTime updatedAt;
}
