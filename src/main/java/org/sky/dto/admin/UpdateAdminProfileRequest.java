package org.sky.dto.admin;

import jakarta.validation.constraints.Pattern;

public record UpdateAdminProfileRequest(
    String businessName,
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    String phone,
    
    String address,
    
    String contactName
) {}
