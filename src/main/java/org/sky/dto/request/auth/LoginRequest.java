package org.sky.dto.request.auth;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.sky.model.UserRole;
@RegisterForReflection
public record LoginRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,
    
    @NotBlank(message = "Password is required")
    String password,
    
    @NotBlank(message = "Device fingerprint is required")
    String deviceFingerprint,
    
    @NotNull(message = "Role is required")
    UserRole role
) {
    // Constructor compacto - validaciones y normalizaciones
    public LoginRequest {
        // Validaciones
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (deviceFingerprint == null || deviceFingerprint.trim().isEmpty()) {
            throw new IllegalArgumentException("Device fingerprint is required");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }
        
        // Normalizaciones
        email = email.trim().toLowerCase();
        password = password.trim();
        deviceFingerprint = deviceFingerprint.trim();
    }
    
    // Constructor de conveniencia
    public static LoginRequest create(String email, String password, String deviceFingerprint, UserRole role) {
        return new LoginRequest(email, password, deviceFingerprint, role);
    }
}
