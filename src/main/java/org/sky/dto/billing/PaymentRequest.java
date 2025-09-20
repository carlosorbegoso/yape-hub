package org.sky.dto.billing;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record PaymentRequest(
    @JsonProperty("planId")
    Long planId,
    
    @JsonProperty("tokensPackage")
    String tokensPackage,
    
    @JsonProperty("paymentMethod")
    String paymentMethod,
    
    @JsonProperty("effectiveDate")
    String effectiveDate,
    
    @JsonProperty("paymentCode")
    String paymentCode,
    
    @JsonProperty("imageBase64")
    String imageBase64
) {
    
    public boolean isSubscriptionPayment() {
        return planId != null;
    }
    
    public boolean isTokenPurchase() {
        return tokensPackage != null && planId == null;
    }
    
    public boolean isValid() {
        return (planId != null) ^ (tokensPackage != null);
    }
}
