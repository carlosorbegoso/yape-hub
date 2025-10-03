package org.sky.dto.response.common;

public record UserInfo(
    Long id,
    String email,
    String businessName,
    String role,
    Boolean isVerified
) {}