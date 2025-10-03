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
) {}
