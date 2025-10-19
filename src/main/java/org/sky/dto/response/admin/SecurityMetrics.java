package org.sky.dto.response.admin;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record SecurityMetrics(
    Long failedLoginAttempts,
    Long suspiciousActivities,
    Long dataBreaches,
    Double securityScore
) {
    public static SecurityMetrics empty() {
        return new SecurityMetrics(0L, 0L, 0L, 0.0);
    }
}
