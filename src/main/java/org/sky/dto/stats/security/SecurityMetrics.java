package org.sky.dto.stats.security;

public record SecurityMetrics(
    Integer totalSecurityIncidents,
    Integer resolvedIncidents,
    Double securityScore,
    String complianceStatus
) {}