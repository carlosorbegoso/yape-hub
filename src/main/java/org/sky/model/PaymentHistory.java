package org.sky.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_history")
public class PaymentHistory extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "admin_id", nullable = false)
    public Long adminId;

    @Column(name = "payment_type", nullable = false, length = 20)
    public String paymentType; // subscription, tokens

    @Column(name = "amount_pen", nullable = false, precision = 10, scale = 2)
    public BigDecimal amountPen;

    @Column(name = "status", nullable = false, length = 20)
    public String status; // pending, completed, failed, refunded

    @Column(name = "payment_method", nullable = false, length = 50)
    public String paymentMethod; // manual_yape, manual_plin, stripe, paypal

    @Column(name = "transaction_id", length = 100)
    public String transactionId;

    @Column(name = "payment_code_id")
    public Long paymentCodeId;

    @Column(name = "manual_payment_id")
    public Long manualPaymentId;

    @Column(name = "plan_id")
    public Long planId;

    @Column(name = "tokens_purchased")
    public Integer tokensPurchased;

    @Column(name = "notes", columnDefinition = "TEXT")
    public String notes;

    @CreationTimestamp
    @Column(name = "created_at")
    public LocalDateTime createdAt;

    // Constructores
    public PaymentHistory() {}

    public PaymentHistory(Long adminId, String paymentType, BigDecimal amountPen, String status, String paymentMethod) {
        this.adminId = adminId;
        this.paymentType = paymentType;
        this.amountPen = amountPen;
        this.status = status;
        this.paymentMethod = paymentMethod;
    }

    // Métodos de utilidad
    public boolean isCompleted() {
        return "completed".equals(this.status);
    }

    public boolean isPending() {
        return "pending".equals(this.status);
    }

    public boolean isFailed() {
        return "failed".equals(this.status);
    }

    public void markAsCompleted(String transactionId) {
        this.status = "completed";
        this.transactionId = transactionId;
    }

    public void markAsFailed(String notes) {
        this.status = "failed";
        this.notes = notes;
    }

    public void markAsRefunded(String notes) {
        this.status = "refunded";
        this.notes = notes;
    }

    // Factory methods
    public static PaymentHistory createSubscriptionPayment(Long adminId, BigDecimal amountPen, Long planId, String paymentMethod) {
        PaymentHistory payment = new PaymentHistory(adminId, "subscription", amountPen, "pending", paymentMethod);
        payment.planId = planId;
        return payment;
    }

    public static PaymentHistory createTokenPayment(Long adminId, BigDecimal amountPen, Integer tokensPurchased, String paymentMethod) {
        PaymentHistory payment = new PaymentHistory(adminId, "tokens", amountPen, "pending", paymentMethod);
        payment.tokensPurchased = tokensPurchased;
        return payment;
    }
}
