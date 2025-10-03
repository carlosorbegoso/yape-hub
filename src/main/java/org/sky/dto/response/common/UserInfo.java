package org.sky.dto.response.common;

public record UserInfo(
    Long id,
    String email,
    String businessName,
    Long businessId,
    String role,
    Boolean isVerified
) {}