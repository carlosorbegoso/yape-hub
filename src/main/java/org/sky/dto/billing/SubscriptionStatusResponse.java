package org.sky.dto.billing;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record SubscriptionStatusResponse(
        @JsonProperty("subscriptionId") Long subscriptionId,
        @JsonProperty("status") String status,
        @JsonProperty("planName") String planName,
        @JsonProperty("description") String description,
        @JsonProperty("price") Double price,
        @JsonProperty("currency") String currency,
        @JsonProperty("billingCycle") String billingCycle,
        @JsonProperty("maxSellers") Integer maxSellers,
        @JsonProperty("startDate") LocalDateTime startDate,
        @JsonProperty("endDate") LocalDateTime endDate,
        @JsonProperty("isActive") Boolean isActive,
        @JsonProperty("message") String message
) {}
