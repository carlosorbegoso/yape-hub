package org.sky.dto.response.qr;

import java.time.LocalDateTime;

public record AffiliationCodeResponse(
    String affiliationCode,
    LocalDateTime expiresAt,
    Integer maxUses,
    Integer remainingUses,
    Long branchId
) {}
