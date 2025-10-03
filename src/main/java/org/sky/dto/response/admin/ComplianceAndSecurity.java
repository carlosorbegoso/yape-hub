package org.sky.dto.response.admin;

public record ComplianceAndSecurity(
    SecurityMetrics securityMetrics,
    ComplianceStatus complianceStatus
) {
    public static ComplianceAndSecurity empty() {
        return new ComplianceAndSecurity(SecurityMetrics.empty(), ComplianceStatus.empty());
    }
}