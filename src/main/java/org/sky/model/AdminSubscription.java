package org.sky.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_subscriptions")
public class AdminSubscription extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "admin_id", nullable = false)
    public Long adminId;

    @Column(name = "plan_id", nullable = false)
    public Long planId;

    @Column(name = "status", nullable = false, length = 20)
    public String status; // active, cancelled, expired, suspended

    @Column(name = "start_date", nullable = false)
    public LocalDateTime startDate;

    @Column(name = "end_date")
    public LocalDateTime endDate;

    @Column(name = "auto_renew", nullable = false)
    public Boolean autoRenew = true;

    @CreationTimestamp
    @Column(name = "created_at")
    public LocalDateTime createdAt;

    // Constructores
    public AdminSubscription() {}

    public AdminSubscription(Long adminId, Long planId, String status, LocalDateTime startDate) {
        this.adminId = adminId;
        this.planId = planId;
        this.status = status;
        this.startDate = startDate;
    }

    // Métodos de utilidad
    public boolean isActive() {
        return "active".equals(this.status) && 
               (this.endDate == null || this.endDate.isAfter(LocalDateTime.now()));
    }

    public boolean isExpired() {
        return this.endDate != null && this.endDate.isBefore(LocalDateTime.now());
    }
}
