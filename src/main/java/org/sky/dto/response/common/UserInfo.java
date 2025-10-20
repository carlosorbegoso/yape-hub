package org.sky.dto.response.common;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.sky.model.UserRole;

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

    // Handle potential null email
    email = email != null ? email.trim().toLowerCase() : null;
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


    UserRole userRole = admin.user.getRoleSafe();

    return new UserInfo(
        admin.user.id,
        admin.user.email, // This might be null
        admin.businessName,
        admin.id,
        userRole != null ? userRole.toString() : null,
        admin.user.isVerified
    );
  }

  // Similar modifications for fromSeller method
  public static UserInfo fromSeller(org.sky.model.SellerEntity seller) {
    if (seller == null || seller.user == null || seller.branch == null || seller.branch.admin == null) {
      throw new IllegalArgumentException("Seller, user, branch, and admin cannot be null");
    }

   UserRole userRole = seller.user.getRoleSafe();

    return new UserInfo(
        seller.user.id,
        seller.user.email, // This might be null
        seller.branch.admin.businessName,
        seller.branch.admin.id,
        userRole != null ? userRole.toString() : null,
        seller.user.isVerified
    );
  }
}