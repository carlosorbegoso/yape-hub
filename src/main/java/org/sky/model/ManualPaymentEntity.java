package org.sky.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "manual_payments")
public class ManualPaymentEntity extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "payment_code_id", nullable = false)
    public Long paymentCodeId;

    @Column(name = "admin_id", nullable = false)
    public Long adminId;

    @Column(name = "image_base64", columnDefinition = "TEXT")
    public String imageBase64;

    @Column(name = "amount_pen", nullable = false, precision = 10, scale = 2)
    public BigDecimal amountPen;

    @Column(name = "yape_number", nullable = false, length = 20)
    public String yapeNumber;

    @Column(name = "status", nullable = false, length = 20)
    public String status = "pending"; // pending, approved, rejected

    @Column(name = "admin_reviewer_id")
    public Long adminReviewerId;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    public String reviewNotes;

    @Column(name = "reviewed_at")
    public LocalDateTime reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at")
    public LocalDateTime createdAt;

    // Constructores
    public ManualPaymentEntity() {}

    public ManualPaymentEntity(Long paymentCodeId, Long adminId, String imageBase64, BigDecimal amountPen, String yapeNumber) {
        this.paymentCodeId = paymentCodeId;
        this.adminId = adminId;
        this.imageBase64 = imageBase64;
        this.amountPen = amountPen;
        this.yapeNumber = yapeNumber;
    }

    // Métodos de utilidad
    public boolean isPending() {
        return "pending".equals(this.status);
    }

    public boolean isApproved() {
        return "approved".equals(this.status);
    }

    public boolean isRejected() {
        return "rejected".equals(this.status);
    }

    public void approve(Long adminReviewerId, String reviewNotes) {
        this.status = "approved";
        this.adminReviewerId = adminReviewerId;
        this.reviewNotes = reviewNotes;
        this.reviewedAt = LocalDateTime.now();
    }

    public void reject(Long adminReviewerId, String reviewNotes) {
        this.status = "rejected";
        this.adminReviewerId = adminReviewerId;
        this.reviewNotes = reviewNotes;
        this.reviewedAt = LocalDateTime.now();
    }
}
