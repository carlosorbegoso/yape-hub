package org.sky.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_codes")
public class PaymentCode extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    public String code;

    @Column(name = "admin_id", nullable = false)
    public Long adminId;

    @Column(name = "plan_id")
    public Long planId;

    @Column(name = "tokens_package", length = 50)
    public String tokensPackage;

    @Column(name = "amount_pen", nullable = false, precision = 10, scale = 2)
    public BigDecimal amountPen;

    @Column(name = "yape_number", nullable = false, length = 20)
    public String yapeNumber;

    @Column(name = "status", nullable = false, length = 20)
    public String status = "pending"; // pending, paid, expired

    @Column(name = "expires_at", nullable = false)
    public LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at")
    public LocalDateTime createdAt;

    // Constructores
    public PaymentCode() {}

    public PaymentCode(String code, Long adminId, BigDecimal amountPen, String yapeNumber, LocalDateTime expiresAt) {
        this.code = code;
        this.adminId = adminId;
        this.amountPen = amountPen;
        this.yapeNumber = yapeNumber;
        this.expiresAt = expiresAt;
    }

    // Métodos de utilidad
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public boolean isPending() {
        return "pending".equals(this.status) && !isExpired();
    }

    public void markAsPaid() {
        this.status = "paid";
    }

    public void markAsExpired() {
        this.status = "expired";
    }

    public String getPaymentType() {
        if (this.planId != null) {
            return "subscription";
        } else if (this.tokensPackage != null) {
            return "tokens";
        }
        return "unknown";
    }
}
