package org.sky.dto.stats;

public record ComplianceAndSecurity(
    SecurityMetrics securityMetrics,
    ComplianceStatus complianceStatus
) {}