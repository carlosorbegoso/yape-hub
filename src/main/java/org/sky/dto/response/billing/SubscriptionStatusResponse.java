package org.sky.dto.response.billing;

import java.time.LocalDateTime;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
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
