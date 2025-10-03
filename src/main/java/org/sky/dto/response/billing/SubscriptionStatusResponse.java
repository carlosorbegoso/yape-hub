package org.sky.dto.response.billing;

import java.time.LocalDateTime;

public record SubscriptionStatusResponse(
        Long subscriptionId,
        String status,
        String planName,
        String description,
        Double price,
        String currency,
        String billingCycle,
        Integer maxSellers,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Boolean isActive,
        String message
) {}
