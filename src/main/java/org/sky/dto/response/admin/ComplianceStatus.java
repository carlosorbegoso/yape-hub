package org.sky.dto.response.admin;

public record ComplianceStatus(
    String dataProtection,
    String auditTrail,
    String backupStatus,
    String lastAudit
) {}