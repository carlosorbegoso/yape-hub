package org.sky.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "branches")
public class Branch extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "admin_id", nullable = false)
    public Admin admin;
    
    @NotBlank
    @Column(nullable = false)
    public String name;
    
    @NotBlank
    @Column(unique = true, nullable = false)
    public String code;
    
    @NotBlank
    @Column(nullable = false)
    public String address;
    
    @Column(name = "is_active")
    public Boolean isActive = true;
    
    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public List<Seller> sellers;
    
    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public List<Transaction> transactions;
    
    @CreationTimestamp
    @Column(name = "created_at")
    public LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    public LocalDateTime updatedAt;
    
    // Constructors
    public Branch() {}
    
    public Branch(Admin admin, String name, String code, String address) {
        this.admin = admin;
        this.name = name;
        this.code = code;
        this.address = address;
    }
}
