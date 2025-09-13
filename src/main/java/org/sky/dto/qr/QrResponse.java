package org.sky.dto.qr;

import java.time.LocalDateTime;

public record QrResponse(
    Long qrId,
    String qrCode,
    String qrImageUrl,
    LocalDateTime expiresAt,
    Integer maxUses,
    Integer remainingUses,
    Long branchId
) {}
