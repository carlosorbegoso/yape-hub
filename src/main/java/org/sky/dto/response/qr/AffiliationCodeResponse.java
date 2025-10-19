package org.sky.dto.response.qr;

import java.time.LocalDateTime;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record AffiliationCodeResponse(
    String affiliationCode,
    LocalDateTime expiresAt,
    Integer maxUses,
    Integer remainingUses,
    Long branchId
) {}
