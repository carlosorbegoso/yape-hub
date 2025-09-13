package org.sky.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public class User extends PanacheEntity {
    
    @NotBlank
    @Email
    @Column(unique = true, nullable = false)
    public String email;
    
    @NotBlank
    @Column(nullable = false)
    public String password;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public UserRole role;
    
    @Column(name = "is_verified")
    public Boolean isVerified = false;
    
    @Column(name = "is_active")
    public Boolean isActive = true;
    
    @Column(name = "last_login")
    public LocalDateTime lastLogin;
    
    @Column(name = "device_fingerprint")
    public String deviceFingerprint;
    
    @CreationTimestamp
    @Column(name = "created_at")
    public LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    public LocalDateTime updatedAt;
    
    public enum UserRole {
        ADMIN, SELLER
    }
    
    // Constructors
    public User() {}
    
    public User(String email, String password, UserRole role) {
        this.email = email;
        this.password = password;
        this.role = role;
    }
}
