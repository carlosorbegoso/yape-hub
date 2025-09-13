package org.sky.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "admins")
public class Admin extends PanacheEntity {
    
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public User user;
    
    @NotBlank
    @Column(name = "business_name", nullable = false)
    public String businessName;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", nullable = false)
    public BusinessType businessType;
    
    @NotBlank
    @Column(unique = true, nullable = false)
    public String ruc;
    
    @NotBlank
    @Column(name = "contact_name", nullable = false)
    public String contactName;
    
    @NotBlank
    @Column(nullable = false)
    public String phone;
    
    @NotBlank
    @Column(nullable = false)
    public String address;
    
    @OneToMany(mappedBy = "admin", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public List<Branch> branches;
    
    @CreationTimestamp
    @Column(name = "created_at")
    public LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    public LocalDateTime updatedAt;
    
    public enum BusinessType {
        RESTAURANT, RETAIL, SERVICES, OTHER
    }
    
    // Constructors
    public Admin() {}
    
    public Admin(User user, String businessName, BusinessType businessType, String ruc, 
                 String contactName, String phone, String address) {
        this.user = user;
        this.businessName = businessName;
        this.businessType = businessType;
        this.ruc = ruc;
        this.contactName = contactName;
        this.phone = phone;
        this.address = address;
    }
}
