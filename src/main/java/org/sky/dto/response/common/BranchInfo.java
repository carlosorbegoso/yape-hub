package org.sky.dto.response.common;

import java.time.LocalDateTime;

public record BranchInfo(
    Long id,
    String name,
    String address,
    String phone,
    Boolean isActive,
    LocalDateTime createdAt
) {}