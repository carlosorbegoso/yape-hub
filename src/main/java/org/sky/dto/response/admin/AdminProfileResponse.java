package org.sky.dto.response.admin;

import org.sky.dto.response.common.BranchInfo;
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
) {}
