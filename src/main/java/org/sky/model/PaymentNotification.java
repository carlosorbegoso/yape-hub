package org.sky.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_notifications")
public class PaymentNotification extends PanacheEntity {
    
    @Column(name = "admin_id", nullable = false)
    public Long adminId;
    
    @Column(name = "amount", nullable = false)
    public Double amount;
    
    @Column(name = "sender_name", nullable = false)
    public String senderName;
    
    @Column(name = "yape_code", nullable = false)
    public String yapeCode;
    
    @Column(name = "status", nullable = false)
    public String status = "PENDING";
    
    @Column(name = "confirmed_by")
    public Long confirmedBy;
    
    @Column(name = "confirmed_at")
    public LocalDateTime confirmedAt;
    
    @Column(name = "rejected_by")
    public Long rejectedBy;
    
    @Column(name = "rejected_at")
    public LocalDateTime rejectedAt;
    
    @Column(name = "rejection_reason")
    public String rejectionReason;
    
    @CreationTimestamp
    @Column(name = "created_at")
    public LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    public LocalDateTime updatedAt;
}
