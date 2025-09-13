package org.sky.dto.auth;

import org.sky.model.User;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    Long expiresIn,
    UserInfo user
) {
    public record UserInfo(
        Long id,
        String email,
        User.UserRole role,
        Long businessId,
        String businessName,
        Boolean isVerified
    ) {}
}
