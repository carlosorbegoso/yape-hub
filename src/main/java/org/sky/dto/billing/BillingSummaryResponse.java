package org.sky.dto.billing;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record BillingSummaryResponse(
        @JsonProperty("totalSpent") Double totalSpent,
        @JsonProperty("currency") String currency,
        @JsonProperty("nextBillingDate") LocalDateTime nextBillingDate,
        @JsonProperty("autoRenewal") Boolean autoRenewal,
        @JsonProperty("paymentMethod") String paymentMethod
) {}
