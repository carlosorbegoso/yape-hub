package org.sky.dto.request.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record AdminRegisterRequest(
    @NotBlank(message = "Business name is required")
    String businessName,
    
    @NotBlank(message = "Business type is required")
    String businessType,
    
    @NotBlank(message = "RUC is required")
    String ruc,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,
    
    @NotBlank(message = "Password is required")
    String password,
    
    @NotBlank(message = "Phone is required")
    String phone,
    
    @NotBlank(message = "Address is required")
    String address,
    
    @NotBlank(message = "Contact name is required")
    String contactName
) {
    // Constructor compacto simplificado - solo normalizaciones básicas
    public AdminRegisterRequest {
        // Normalizaciones simples (sin validaciones que causen errores)
        if (businessName != null) businessName = businessName.trim();
        if (businessType != null) businessType = businessType.trim().toUpperCase();
        if (ruc != null) ruc = ruc.trim();
        if (email != null) email = email.trim().toLowerCase();
        if (password != null) password = password.trim();
        if (phone != null) phone = phone.trim();
        if (address != null) address = address.trim();
        if (contactName != null) contactName = contactName.trim();
    }
}
