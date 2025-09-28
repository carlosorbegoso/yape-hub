package org.sky.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_rejections")
public class PaymentRejection extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    
    @Column(name = "payment_notification_id", nullable = false)
    public Long paymentNotificationId;
    
    @Column(name = "seller_id", nullable = false)
    public Long sellerId;
    
    @Column(name = "rejection_reason")
    public String rejectionReason;
    
    @CreationTimestamp
    @Column(name = "rejected_at")
    public LocalDateTime rejectedAt;
    
    // Constructors
    public PaymentRejection() {}
    
    public PaymentRejection(Long paymentNotificationId, Long sellerId, String rejectionReason) {
        this.paymentNotificationId = paymentNotificationId;
        this.sellerId = sellerId;
        this.rejectionReason = rejectionReason;
    }
}
