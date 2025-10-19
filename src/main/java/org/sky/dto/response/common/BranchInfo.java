package org.sky.dto.response.common;

import java.time.LocalDateTime;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record BranchInfo(
    Long id,
    String name,
    String address,
    String phone,
    Boolean isActive,
    LocalDateTime createdAt
) {
    // Constructor compacto - validaciones y normalizaciones
    public BranchInfo {
        // Validaciones
        if (id == null) {
            throw new IllegalArgumentException("Branch ID cannot be null");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Branch name cannot be null or empty");
        }
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("Branch address cannot be null or empty");
        }
        
        // Normalizaciones
        name = name.trim();
        address = address.trim();
        phone = phone != null ? phone.trim() : null;
        
        // Valores por defecto
        if (isActive == null) isActive = true;
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
    
    // Constructor desde BranchEntity
    public static BranchInfo fromBranch(org.sky.model.BranchEntity branch) {
        if (branch == null) {
            throw new IllegalArgumentException("Branch entity cannot be null");
        }
        
        return new BranchInfo(
            branch.id,
            branch.name,
            branch.address,
            null, // phone no está en BranchEntity
            branch.isActive,
            branch.createdAt
        );
    }
}
