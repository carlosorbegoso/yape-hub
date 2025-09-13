package org.sky.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "sellers")
public class Seller extends PanacheEntity {
    
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    public User user;
    
    @NotBlank
    @Column(name = "seller_name", nullable = false)
    public String sellerName;
    
    @Email
    @Column(unique = true, nullable = false)
    public String email;
    
    @NotBlank
    @Column(unique = true, nullable = false)
    public String phone;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "branch_id", nullable = false)
    public Branch branch;
    
    @Column(name = "is_active")
    public Boolean isActive = true;
    
    @Column(name = "is_online")
    public Boolean isOnline = false;
    
    @Column(name = "affiliation_code")
    public String affiliationCode;
    
    @Column(name = "affiliation_date")
    public LocalDateTime affiliationDate;
    
    @Column(name = "total_payments")
    public Integer totalPayments = 0;
    
    @Column(name = "total_amount", precision = 10, scale = 2)
    public BigDecimal totalAmount = BigDecimal.ZERO;
    
    @Column(name = "last_payment")
    public LocalDateTime lastPayment;
    
    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public List<Transaction> transactions;
    
    @CreationTimestamp
    @Column(name = "created_at")
    public LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    public LocalDateTime updatedAt;
    
    // Constructors
    public Seller() {}
    
    public Seller(User user, String sellerName, String email, String phone, Branch branch, String affiliationCode) {
        this.user = user;
        this.sellerName = sellerName;
        this.email = email;
        this.phone = phone;
        this.branch = branch;
        this.affiliationCode = affiliationCode;
        this.affiliationDate = LocalDateTime.now();
    }
}
