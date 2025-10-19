package org.sky.dto.request.billing;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record PaymentRequest(
    Long planId,
    String paymentMethod,
    String effectiveDate,
    String paymentCode,
    String imageBase64
) {
    
    public boolean isSubscriptionPayment() {
        return planId != null;
    }
    
    public boolean isValid() {
        return planId != null;
    }
}
