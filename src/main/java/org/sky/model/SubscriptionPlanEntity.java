package org.sky.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlanEntity extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "name", nullable = false, length = 100)
    public String name;

    @Column(name = "description", columnDefinition = "TEXT")
    public String description;

    @Column(name = "price_pen", nullable = false, precision = 10, scale = 2)
    public BigDecimal pricePen;

    @Column(name = "billing_cycle", nullable = false, length = 20)
    public String billingCycle; // monthly, yearly

    @Column(name = "max_admins", nullable = false)
    public Integer maxAdmins = 1;

    @Column(name = "max_sellers", nullable = false)
    public Integer maxSellers = 2;

    @Column(name = "features", columnDefinition = "JSONB")
    public String features; // JSON string con features del plan

    @Column(name = "is_active", nullable = false)
    public Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at")
    public LocalDateTime createdAt;

    // Constructores
    public SubscriptionPlanEntity() {}

    public SubscriptionPlanEntity(String name, String description, BigDecimal pricePen, String billingCycle,
                                  Integer maxAdmins, Integer maxSellers) {
        this.name = name;
        this.description = description;
        this.pricePen = pricePen;
        this.billingCycle = billingCycle;
        this.maxAdmins = maxAdmins;
        this.maxSellers = maxSellers;
    }
}
