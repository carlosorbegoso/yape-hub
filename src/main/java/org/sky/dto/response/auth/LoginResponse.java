package org.sky.dto.response.auth;

import org.sky.dto.response.common.UserInfo;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    Long expiresIn,
    UserInfo user
) {
    // Constructor compacto - validaciones y normalizaciones
    public LoginResponse {
        // Validaciones
        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Access token cannot be null or empty");
        }
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Refresh token cannot be null or empty");
        }
        if (user == null) {
            throw new IllegalArgumentException("User info cannot be null");
        }
        if (expiresIn == null || expiresIn <= 0) {
            expiresIn = 3600L; // Default 1 hour
        }
        
        // Normalizaciones
        accessToken = accessToken.trim();
        refreshToken = refreshToken.trim();
    }
    
    // Constructor de conveniencia para casos comunes
    public static LoginResponse create(String accessToken, String refreshToken, UserInfo user) {
        return new LoginResponse(accessToken, refreshToken, 3600L, user);
    }
    
    public static LoginResponse create(String accessToken, String refreshToken, UserInfo user, Long expiresIn) {
        return new LoginResponse(accessToken, refreshToken, expiresIn, user);
    }
}
