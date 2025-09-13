package org.sky.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction extends PanacheEntity {
    
    @NotBlank
    @Column(name = "security_code", unique = true, nullable = false)
    public String securityCode;
    
    @NotNull
    @Column(precision = 10, scale = 2, nullable = false)
    public BigDecimal amount;
    
    @Column(name = "transaction_timestamp", nullable = false)
    public LocalDateTime timestamp;
    
    @Column(columnDefinition = "TEXT")
    public String description;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public TransactionType type;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    public Branch branch;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    public Seller seller;
    
    @Column(name = "is_processed")
    public Boolean isProcessed = false;
    
    @Column(name = "is_confirmed")
    public Boolean isConfirmed = false;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    public PaymentMethod paymentMethod;
    
    @Column(name = "customer_phone")
    public String customerPhone;
    
    @Column(name = "processed_at")
    public LocalDateTime processedAt;
    
    @Column(name = "confirmed_at")
    public LocalDateTime confirmedAt;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    public String notes;
    
    @CreationTimestamp
    @Column(name = "created_at")
    public LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    public LocalDateTime updatedAt;
    
    public enum TransactionType {
        PAYMENT, REFUND, COMMISSION
    }
    
    public enum PaymentMethod {
        YAPE, PLIN, BIM, CASH, CARD
    }
    
    // Constructors
    public Transaction() {}
    
    public Transaction(String securityCode, BigDecimal amount, LocalDateTime timestamp, 
                      String description, TransactionType type, Branch branch, PaymentMethod paymentMethod) {
        this.securityCode = securityCode;
        this.amount = amount;
        this.timestamp = timestamp;
        this.description = description;
        this.type = type;
        this.branch = branch;
        this.paymentMethod = paymentMethod;
    }
}
