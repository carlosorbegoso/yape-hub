package org.sky.dto.response.branch;

import java.time.LocalDateTime;

public record BranchResponse(
    Long branchId,
    String name,
    String code,
    String address,
    Boolean isActive,
    Long sellersCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    // Constructor compacto - validaciones y normalizaciones
    public BranchResponse {
        // Validaciones
        if (branchId == null) {
            throw new IllegalArgumentException("Branch ID cannot be null");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Branch name cannot be null or empty");
        }
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Branch code cannot be null or empty");
        }
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("Branch address cannot be null or empty");
        }
        
        // Normalizaciones
        name = name.trim();
        code = code.trim().toUpperCase();
        address = address.trim();
        
        // Valores por defecto
        if (isActive == null) isActive = true;
        if (sellersCount == null) sellersCount = 0L;
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }
    
    // Constructor desde BranchEntity
    public static BranchResponse fromBranch(org.sky.model.BranchEntity branch) {
        if (branch == null) {
            throw new IllegalArgumentException("Branch entity cannot be null");
        }
        
        return new BranchResponse(
            branch.id,
            branch.name,
            branch.code,
            branch.address,
            branch.isActive,
            branch.sellers != null ? (long) branch.sellers.size() : 0L,
            branch.createdAt,
            branch.updatedAt
        );
    }
    
    // Constructor con sellers count personalizado
    public static BranchResponse withSellersCount(org.sky.model.BranchEntity branch, Long customSellersCount) {
        if (branch == null) {
            throw new IllegalArgumentException("Branch entity cannot be null");
        }
        
        return new BranchResponse(
            branch.id,
            branch.name,
            branch.code,
            branch.address,
            branch.isActive,
            customSellersCount != null ? customSellersCount : 0L,
            branch.createdAt,
            branch.updatedAt
        );
    }
}
