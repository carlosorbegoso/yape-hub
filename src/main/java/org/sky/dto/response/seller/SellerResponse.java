package org.sky.dto.response.seller;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SellerResponse(
    Long sellerId,
    String name,
    String email,
    String phone,
    Long branchId,
    String branchName,
    Boolean isActive,
    Boolean isOnline,
    Integer totalPayments,
    BigDecimal totalAmount,
    LocalDateTime lastPayment,
    LocalDateTime affiliationDate
) {
    // Constructor compacto - validaciones y normalizaciones
    public SellerResponse {
        // Validaciones
        if (sellerId == null) {
            throw new IllegalArgumentException("Seller ID cannot be null");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Seller name cannot be null or empty");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Seller email cannot be null or empty");
        }
        if (branchId == null) {
            throw new IllegalArgumentException("Branch ID cannot be null");
        }
        
        // Normalizaciones
        name = name.trim();
        email = email.trim().toLowerCase();
        phone = phone != null ? phone.trim() : null;
        branchName = branchName != null ? branchName.trim() : null;
        
        // Valores por defecto
        if (isActive == null) isActive = true;
        if (isOnline == null) isOnline = false;
        if (totalPayments == null) totalPayments = 0;
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;
    }
    
    // Constructor desde SellerEntity
    public static SellerResponse fromSeller(org.sky.model.SellerEntity seller) {
        if (seller == null || seller.user == null || seller.branch == null) {
            throw new IllegalArgumentException("Seller, user, and branch cannot be null");
        }
        
        return new SellerResponse(
            seller.id,
            seller.user.email, // Usar email como name temporal
            seller.user.email,
            null, // phone no está en UserEntityEntity
            seller.branch.id,
            seller.branch.name,
            seller.user.isActive,
            false, // isOnline - se puede calcular dinámicamente
            0, // totalPayments - se puede calcular dinámicamente
            BigDecimal.ZERO, // totalAmount - se puede calcular dinámicamente
            null, // lastPayment - se puede calcular dinámicamente
            seller.createdAt
        );
    }
    
    // Constructor con estadísticas
    public static SellerResponse withStats(org.sky.model.SellerEntity seller, 
                                         Integer totalPayments, 
                                         BigDecimal totalAmount, 
                                         LocalDateTime lastPayment,
                                         Boolean isOnline) {
        if (seller == null || seller.user == null || seller.branch == null) {
            throw new IllegalArgumentException("Seller, user, and branch cannot be null");
        }
        
        return new SellerResponse(
            seller.id,
            seller.user.email, // Usar email como name temporal
            seller.user.email,
            null, // phone no está en UserEntityEntity
            seller.branch.id,
            seller.branch.name,
            seller.user.isActive,
            isOnline != null ? isOnline : false,
            totalPayments != null ? totalPayments : 0,
            totalAmount != null ? totalAmount : BigDecimal.ZERO,
            lastPayment,
            seller.createdAt
        );
    }
}
