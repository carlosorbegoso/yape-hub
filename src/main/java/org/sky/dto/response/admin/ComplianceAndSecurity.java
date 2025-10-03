package org.sky.dto.response.admin;

public record ComplianceAndSecurity(
    SecurityMetrics securityMetrics,
    ComplianceStatus complianceStatus
) {}