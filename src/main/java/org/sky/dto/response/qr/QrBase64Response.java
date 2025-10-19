package org.sky.dto.response.qr;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record QrBase64Response(
    String affiliationCode,
    String qrBase64,
    String expiresAt,
    Integer maxUses,
    Integer remainingUses,
    String branchName,
    String adminName
) {}
