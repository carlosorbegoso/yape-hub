package org.sky.dto.response.stats;

public record UserEngagement(
    Long dailyActiveUsers,
    Long weeklyActiveUsers,
    Long monthlyActiveUsers,
    Double averageSessionDuration,
    FeatureUsage featureUsage
) {
    public static UserEngagement empty() {
        return new UserEngagement(0L, 0L, 0L, 0.0, FeatureUsage.empty());
    }
}