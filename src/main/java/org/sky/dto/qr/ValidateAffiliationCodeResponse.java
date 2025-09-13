package org.sky.dto.qr;

import java.time.LocalDateTime;

public record ValidateAffiliationCodeResponse(
    Boolean isValid,
    String businessName,
    String branchName,
    LocalDateTime expiresAt
) {}
