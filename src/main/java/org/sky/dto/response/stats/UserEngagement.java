package org.sky.dto.response.stats;

public record UserEngagement(
    Long dailyActiveUsers,
    Long weeklyActiveUsers,
    Long monthlyActiveUsers,
    Double averageSessionDuration,
    FeatureUsage featureUsage
) {}