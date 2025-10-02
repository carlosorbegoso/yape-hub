package org.sky.dto.stats.security;

public record ComplianceStatus(
    String dataProtection,
    String auditTrail,
    String backupStatus,
    String lastAudit
) {}