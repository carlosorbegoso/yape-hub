package org.sky.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "qr_codes")
public class QrCode extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    
    @NotBlank
    @Column(name = "qr_data", unique = true, nullable = false)
    public String qrData;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public QrType type;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    public BranchEntity branch;
    
    @Column(name = "expires_at")
    public LocalDateTime expiresAt;
    
    @Column(name = "max_uses")
    public Integer maxUses;
    
    @Column(name = "remaining_uses")
    public Integer remainingUses;
    
    @Column(name = "is_active")
    public Boolean isActive = true;
    
    @Column(name = "qr_image_url")
    public String qrImageUrl;
    
    @CreationTimestamp
    @Column(name = "created_at")
    public LocalDateTime createdAt;
    
    public enum QrType {
        AFFILIATION, PAYMENT, PROMOTION
    }
    
    // Constructors
    public QrCode() {}
    
    public QrCode(String qrData, QrType type, BranchEntity branch, LocalDateTime expiresAt, Integer maxUses) {
        this.qrData = qrData;
        this.type = type;
        this.branch = branch;
        this.expiresAt = expiresAt;
        this.maxUses = maxUses;
        this.remainingUses = maxUses;
    }
    
}
