package org.sky.dto.response.admin;

import org.sky.dto.response.common.BranchInfo;
import org.sky.model.BusinessType;

import java.time.LocalDateTime;
import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record AdminProfileResponse(
    Long id,
    String email,
    String businessName,
    BusinessType businessType,
    String ruc,
    String phone,
    String address,
    String contactName,
    Boolean isVerified,
    LocalDateTime createdAt,
    List<BranchInfo> branches
) {
    // Constructor compacto - validaciones y normalizaciones
    public AdminProfileResponse {
        // Validaciones
        if (id == null) {
            throw new IllegalArgumentException("Admin ID cannot be null");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (businessName == null || businessName.trim().isEmpty()) {
            throw new IllegalArgumentException("Business name cannot be null or empty");
        }
        if (businessType == null) {
            throw new IllegalArgumentException("Business type cannot be null");
        }
        
        // Normalizaciones
        email = email.trim().toLowerCase();
        businessName = businessName.trim();
        ruc = ruc != null ? ruc.trim() : null;
        phone = phone != null ? phone.trim() : null;
        address = address != null ? address.trim() : null;
        contactName = contactName != null ? contactName.trim() : null;
        
        // Valores por defecto
        if (isVerified == null) isVerified = false;
        if (branches == null) branches = List.of();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
    
    // Constructor desde AdminEntity
    public static AdminProfileResponse fromAdmin(org.sky.model.AdminEntity admin) {
        if (admin == null || admin.user == null) {
            throw new IllegalArgumentException("Admin and user cannot be null");
        }
        
        return new AdminProfileResponse(
            admin.id,
            admin.user.email,
            admin.businessName,
            admin.businessType,
            admin.ruc,
            null, // phone no está en UserEntityEntity
            admin.address,
            admin.contactName,
            admin.user.isVerified,
            admin.createdAt,
            admin.branches != null ? admin.branches.stream()
                .map(branch -> new BranchInfo(
                    branch.id, branch.name, branch.address, null, // phone no está en BranchEntity
                    branch.isActive, branch.createdAt))
                .toList() : List.of()
        );
    }

}
