package org.sky.dto.request.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

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
) {}
