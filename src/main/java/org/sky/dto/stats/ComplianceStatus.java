package org.sky.dto.stats;

public record ComplianceStatus(
    String dataProtection,
    String auditTrail,
    String backupStatus,
    String lastAudit
) {}