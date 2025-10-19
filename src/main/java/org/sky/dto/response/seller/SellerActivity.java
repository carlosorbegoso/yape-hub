package org.sky.dto.response.seller;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record SellerActivity(
    Long dailyActiveSellers,
    Long weeklyActiveSellers,
    Long monthlyActiveSellers,
    Double averageSessionDuration,
    Double averageTransactionsPerSeller
) {
    public static SellerActivity empty() {
        return new SellerActivity(0L, 0L, 0L, 0.0, 0.0);
    }
}
