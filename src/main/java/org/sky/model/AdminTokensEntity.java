package org.sky.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_tokens")
public class AdminTokensEntity extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "admin_id", nullable = false, unique = true)
    public Long adminId;

    @Column(name = "tokens_available", nullable = false)
    public Integer tokensAvailable = 0;

    @Column(name = "tokens_used", nullable = false)
    public Integer tokensUsed = 0;

    @Column(name = "tokens_purchased", nullable = false)
    public Integer tokensPurchased = 0;

    @Column(name = "last_reset_date", nullable = false)
    public LocalDate lastResetDate;

    @CreationTimestamp
    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    // Constructores
    public AdminTokensEntity() {}

    public AdminTokensEntity(Long adminId, Integer tokensAvailable, LocalDate lastResetDate) {
        this.adminId = adminId;
        this.tokensAvailable = tokensAvailable;
        this.lastResetDate = lastResetDate;
    }

    // Métodos de utilidad
    public boolean hasEnoughTokens(int tokensNeeded) {
        return this.tokensAvailable >= tokensNeeded;
    }

    public void consumeTokens(int tokensToConsume) {
        if (hasEnoughTokens(tokensToConsume)) {
            this.tokensAvailable -= tokensToConsume;
            this.tokensUsed += tokensToConsume;
            this.updatedAt = LocalDateTime.now();
        } else {
            throw new IllegalStateException("Tokens insuficientes");
        }
    }

    public void addTokens(int tokensToAdd) {
        this.tokensAvailable += tokensToAdd;
        this.tokensPurchased += tokensToAdd;
        this.updatedAt = LocalDateTime.now();
    }

    public void resetMonthlyTokens(int newTokens) {
        this.tokensAvailable = newTokens;
        this.tokensUsed = 0;
        this.lastResetDate = LocalDate.now();
        this.updatedAt = LocalDateTime.now();
    }
}
