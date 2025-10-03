package org.sky.dto.response.admin;

public record SecurityMetrics(
    Long failedLoginAttempts,
    Long suspiciousActivities,
    Long dataBreaches,
    Double securityScore
) {}