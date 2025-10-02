package org.sky.dto.stats.security;

public record ComplianceAndSecurityResponse(
    SecurityMetrics securityMetrics,
    ComplianceStatus complianceStatus
) {
    
    public record SecurityMetrics(
        Long failedLoginAttempts,
        Long suspiciousActivities,
        Long dataBreaches,
        Double securityScore
    ) {}
    
    public record ComplianceStatus(
        String dataProtection,
        String auditTrail,
        String backupStatus,
        String lastAudit
    ) {}
}
