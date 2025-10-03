package org.sky.dto.response.admin;

public record ComplianceAndSecurityResponse(
    SecurityMetrics securityMetrics,
    ComplianceStatus complianceStatus
) {
    

    

}
