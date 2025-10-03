package org.sky.dto.response.auth;

import org.sky.dto.response.common.UserInfo;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    Long expiresIn,
    UserInfo user
) {}
