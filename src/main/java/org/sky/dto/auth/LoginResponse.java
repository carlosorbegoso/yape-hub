package org.sky.dto.auth;

import org.sky.model.UserEntity;
import org.sky.model.UserRole;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    Long expiresIn,
    UserInfo user
) {
    public record UserInfo(
        Long id,
        String email,
        UserRole role,
        Long businessId,
        String businessName,
        Boolean isVerified,
        Long sellerId
    ) {}
}
