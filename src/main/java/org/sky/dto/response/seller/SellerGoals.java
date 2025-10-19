package org.sky.dto.response.seller;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record SellerGoals(
    Double dailyTarget,
    Double weeklyTarget,
    Double monthlyTarget,
    Double yearlyTarget,
    Double achievementRate,
    Double dailyProgress,
    Double weeklyProgress,
    Double monthlyProgress
) {
    public static SellerGoals empty() {
        return new SellerGoals(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    }
}
