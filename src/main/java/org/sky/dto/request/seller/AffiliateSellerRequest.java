package org.sky.dto.request.seller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AffiliateSellerRequest(
    @NotBlank(message = "Seller name is required")
    String sellerName,
    
    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    String phone,
    
    @NotBlank(message = "Affiliation code is required")
    String affiliationCode
) {}
