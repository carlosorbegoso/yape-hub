package org.sky.dto.response.qr;

public record QrBase64Response(
    String affiliationCode,
    String qrBase64,
    String expiresAt,
    Integer maxUses,
    Integer remainingUses,
    String branchName,
    String adminName
) {}
