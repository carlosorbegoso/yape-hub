package org.sky.dto.qr;

import java.time.LocalDateTime;

public record AffiliationCodeResponse(
    String affiliationCode,
    LocalDateTime expiresAt,
    Integer maxUses,
    Integer remainingUses,
    Long branchId
) {}
