package org.sky.dto.stats;

public record SecurityMetrics(
    Long failedLoginAttempts,
    Long suspiciousActivities,
    Long dataBreaches,
    Double securityScore
) {}