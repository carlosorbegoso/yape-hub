package org.sky.dto.stats;

public record UserEngagement(
    Long dailyActiveUsers,
    Long weeklyActiveUsers,
    Long monthlyActiveUsers,
    Double averageSessionDuration,
    FeatureUsage featureUsage
) {}