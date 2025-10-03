package org.sky.dto.response.seller;

public record SellerGoals(
    Double dailyTarget,
    Double weeklyTarget,
    Double monthlyTarget,
    Double yearlyTarget,
    Double achievementRate,
    Double dailyProgress,
    Double weeklyProgress,
    Double monthlyProgress
) {}