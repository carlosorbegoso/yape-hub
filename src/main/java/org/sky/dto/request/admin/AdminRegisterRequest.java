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
    @Pattern(regexp = "^(RESTAURANT|RETAIL|SERVICES|OTHER)$", message = "Business type must be one of: RESTAURANT, RETAIL, SERVICES, OTHER")
    String businessType,
    
    @NotBlank(message = "RUC is required")
    //@Pattern(regexp = "^[0-9]{11}$", message = "RUC must be 11 digits")
    String ruc,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,
    
    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", 
             message = "Password must be at least 8 characters with uppercase, lowercase, number and special character")
    String password,
    
    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    String phone,
    
    @NotBlank(message = "Address is required")
    String address,
    
    @NotBlank(message = "Contact name is required")
    String contactName
) {
    // Constructor compacto - validaciones y normalizaciones
    public AdminRegisterRequest {
        // Validaciones
        if (businessName == null || businessName.trim().isEmpty()) {
            throw new IllegalArgumentException("Business name is required");
        }
        if (businessType == null || businessType.trim().isEmpty()) {
            throw new IllegalArgumentException("Business type is required");
        }
        if (ruc == null || ruc.trim().isEmpty()) {
            throw new IllegalArgumentException("RUC is required");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone is required");
        }
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("Address is required");
        }
        if (contactName == null || contactName.trim().isEmpty()) {
            throw new IllegalArgumentException("Contact name is required");
        }
        
        // Normalizaciones
        businessName = businessName.trim();
        businessType = businessType.trim().toUpperCase();
        ruc = ruc.trim();
        email = email.trim().toLowerCase();
        password = password.trim();
        phone = phone.trim();
        address = address.trim();
        contactName = contactName.trim();
        
        // Validaciones de formato
        if (!businessType.matches("^(RESTAURANT|RETAIL|SERVICES|OTHER)$")) {
            throw new IllegalArgumentException("Business type must be one of: RESTAURANT, RETAIL, SERVICES, OTHER");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$")) {
            throw new IllegalArgumentException("Password must be at least 8 characters with uppercase, lowercase, number and special character");
        }
        if (!phone.matches("^\\+?[1-9]\\d{1,14}$")) {
            throw new IllegalArgumentException("Invalid phone number format");
        }
    }
    
    // Constructor de conveniencia
    public static AdminRegisterRequest create(String businessName, String businessType, String ruc, 
                                            String email, String password, String phone, 
                                            String address, String contactName) {
        return new AdminRegisterRequest(businessName, businessType, ruc, email, password, phone, address, contactName);
    }
}
