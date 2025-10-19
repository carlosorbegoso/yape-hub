package org.sky.dto.response.admin;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record ComplianceAndSecurityResponse(
    SecurityMetrics securityMetrics,
    ComplianceStatus complianceStatus
) {
    

    

}
