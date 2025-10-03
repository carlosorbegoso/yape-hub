package org.sky.dto.response.common;

public record UserInfo(
    Long id,
    String email,
    String businessName,
    Long businessId,
    String role,
    Boolean isVerified
) {
    // Constructor compacto - validaciones y normalizaciones
    public UserInfo {
        // Validaciones
        if (id == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        
        // Normalizaciones
        email = email.trim().toLowerCase();
        businessName = businessName != null ? businessName.trim() : null;
        role = role != null ? role.toUpperCase() : null;
        
        // Valores por defecto
        if (isVerified == null) {
            isVerified = false;
        }
    }
    
    // Constructor para Admin
    public static UserInfo fromAdmin(org.sky.model.AdminEntity admin) {
        if (admin == null || admin.user == null) {
            throw new IllegalArgumentException("Admin and user cannot be null");
        }
        
        return new UserInfo(
            admin.user.id,
            admin.user.email,
            admin.businessName,
            admin.id,
            admin.user.role.toString(),
            admin.user.isVerified
        );
    }
    
    // Constructor para Seller
    public static UserInfo fromSeller(org.sky.model.SellerEntity seller) {
        if (seller == null || seller.user == null || seller.branch == null || seller.branch.admin == null) {
            throw new IllegalArgumentException("Seller, user, branch, and admin cannot be null");
        }
        
        return new UserInfo(
            seller.user.id,
            seller.user.email,
            seller.branch.admin.businessName,
            seller.branch.admin.id,
            seller.user.role.toString(),
            seller.user.isVerified
        );
    }
}