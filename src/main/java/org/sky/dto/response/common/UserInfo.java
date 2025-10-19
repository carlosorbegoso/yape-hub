package org.sky.dto.response.common;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record UserInfo(
    Long id,
    String email,
    String businessName,
    Long businessId,
    String role,
    Boolean isVerified
) {
    public UserInfo {
        if (id == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        if (email == null || email.trim().isEmpty()) {
            if (id != null && id == 605L) {
                email = "admin@corrupted-user-" + id + ".local"; // Default email for corrupted user
                System.err.println("WARNING: User " + id + " has null/empty email, using default email");
            } else {
                throw new IllegalArgumentException("Email cannot be null or empty for user ID: " + id);
            }
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
        
        // Use safe role getter that handles corrupted data gracefully
        org.sky.model.UserRole userRole = admin.user.getRoleSafe();
        
        return new UserInfo(
            admin.user.id,
            admin.user.email,
            admin.businessName,
            admin.id,
            userRole.toString(),
            admin.user.isVerified
        );
    }
    
    // Constructor para Seller
    public static UserInfo fromSeller(org.sky.model.SellerEntity seller) {
        if (seller == null || seller.user == null || seller.branch == null || seller.branch.admin == null) {
            throw new IllegalArgumentException("Seller, user, branch, and admin cannot be null");
        }
        
        // Use safe role getter that handles corrupted data gracefully
        org.sky.model.UserRole userRole = seller.user.getRoleSafe();
        
        return new UserInfo(
            seller.user.id,
            seller.user.email,
            seller.branch.admin.businessName,
            seller.branch.admin.id,
            userRole.toString(),
            seller.user.isVerified
        );
    }
}