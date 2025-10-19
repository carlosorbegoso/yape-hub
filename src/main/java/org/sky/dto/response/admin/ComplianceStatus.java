package org.sky.dto.response.admin;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record ComplianceStatus(
    String dataProtection,
    String auditTrail,
    String backupStatus,
    String lastAudit
) {
    public static ComplianceStatus empty() {
        return new ComplianceStatus("", "", "", "");
    }
}
