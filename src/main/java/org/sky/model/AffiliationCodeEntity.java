package org.sky.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "affiliation_codes")
public class AffiliationCodeEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    @Column(name = "affiliation_code", unique = true, nullable = false)
    public String affiliationCode;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    public BranchEntity branch;
    
    @Column(name = "expires_at")
    public LocalDateTime expiresAt;
    
    @Column(name = "max_uses")
    public Integer maxUses;
    
    @Column(name = "remaining_uses")
    public Integer remainingUses;
    
    @Column(name = "is_active")
    public Boolean isActive = true;
    
    @Column(columnDefinition = "TEXT")
    public String notes;
    
    @CreationTimestamp
    @Column(name = "created_at")
    public LocalDateTime createdAt;
    
    // Constructors
    public AffiliationCodeEntity() {}
    
    public AffiliationCodeEntity(String affiliationCode, BranchEntity branch, LocalDateTime expiresAt, Integer maxUses) {
        this.affiliationCode = affiliationCode;
        this.branch = branch;
        this.expiresAt = expiresAt;
        this.maxUses = maxUses;
        this.remainingUses = maxUses;
    }
    
}
