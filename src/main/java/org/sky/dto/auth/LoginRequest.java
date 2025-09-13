package org.sky.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.sky.model.User;

public record LoginRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,
    
    @NotBlank(message = "Password is required")
    String password,
    
    @NotBlank(message = "Device fingerprint is required")
    String deviceFingerprint,
    
    @NotNull(message = "Role is required")
    User.UserRole role
) {}
