package org.sky.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "token_packages")
public class TokenPackage extends PanacheEntity {
    
    @Column(name = "package_id", unique = true, nullable = false)
    public String packageId;
    
    @Column(name = "name", nullable = false)
    public String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    public String description;
    
    @Column(name = "tokens", nullable = false)
    public Integer tokens;
    
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    public BigDecimal price;
    
    @Column(name = "currency", nullable = false, length = 3)
    public String currency;
    
    @Column(name = "discount", precision = 5, scale = 4)
    public BigDecimal discount;
    
    @Column(name = "is_popular")
    public Boolean isPopular;
    
    @Column(name = "features", columnDefinition = "TEXT")
    public String features; // JSON string con las características
    
    @Column(name = "is_active")
    public Boolean isActive;
    
    @Column(name = "sort_order")
    public Integer sortOrder;
    
    @Column(name = "created_at")
    public LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    public LocalDateTime updatedAt;
    
    // Constructor por defecto
    public TokenPackage() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isActive = true;
        this.isPopular = false;
        this.discount = BigDecimal.ZERO;
        this.sortOrder = 0;
    }
    
    // Constructor con parámetros principales
    public TokenPackage(String packageId, String name, String description, Integer tokens, 
                       BigDecimal price, String currency, BigDecimal discount, 
                       Boolean isPopular, String features, Integer sortOrder) {
        this();
        this.packageId = packageId;
        this.name = name;
        this.description = description;
        this.tokens = tokens;
        this.price = price;
        this.currency = currency;
        this.discount = discount;
        this.isPopular = isPopular;
        this.features = features;
        this.sortOrder = sortOrder;
    }
    
    // Método para obtener el precio con descuento aplicado
    public BigDecimal getDiscountedPrice() {
        if (discount == null || discount.compareTo(BigDecimal.ZERO) == 0) {
            return price;
        }
        BigDecimal discountAmount = price.multiply(discount);
        return price.subtract(discountAmount);
    }
    
    // Método para verificar si el paquete está activo
    public boolean isActivePackage() {
        return isActive != null && isActive;
    }
}
