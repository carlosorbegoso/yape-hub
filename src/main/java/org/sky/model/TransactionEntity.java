package org.sky.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class TransactionEntity extends PanacheEntityBase {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;
    
    @Column(name = "transaction_id", unique = true, nullable = false)
    public String transactionId;
    
    @Column(name = "amount", nullable = false)
    public Double amount;
    
    @Column(name = "sender_phone", nullable = false)
    public String senderPhone;
    
    @Column(name = "receiver_phone", nullable = false)
    public String receiverPhone;
    
    @Column(name = "status", nullable = false)
    public String status;
    
    @Column(name = "payment_method", nullable = false)
    public String paymentMethod = "YAPE";
    
    @Column(name = "security_code", nullable = false)
    public String securityCode = "SEC123";
    
    @Column(name = "transaction_timestamp", nullable = false)
    public LocalDateTime transactionTimestamp = LocalDateTime.now();
    
    @Column(name = "type", nullable = false)
    public String type = "PAYMENT";
    
    @Column(name = "admin_id", nullable = false)
    public Long adminId;
    
    @Column(name = "yape_notification_id")
    public Long yapeNotificationId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    public BranchEntity branch;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    public SellerEntity seller;
    
    @CreationTimestamp
    @Column(name = "created_at")
    public LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    public LocalDateTime updatedAt;
}