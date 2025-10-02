package org.sky.dto.admin;

import org.sky.model.AdminEntity;
import org.sky.model.BusinessType;

import java.time.LocalDateTime;
import java.util.List;

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
    public record BranchInfo(
        Long id,
        String name,
        String code,
        String address,
        Boolean isActive
    ) {}
}
