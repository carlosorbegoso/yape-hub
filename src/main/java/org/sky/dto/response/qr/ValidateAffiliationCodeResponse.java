package org.sky.dto.response.qr;

import java.time.LocalDateTime;

public record ValidateAffiliationCodeResponse(
    Boolean isValid,
    String businessName,
    String branchName,
    LocalDateTime expiresAt
) {}
