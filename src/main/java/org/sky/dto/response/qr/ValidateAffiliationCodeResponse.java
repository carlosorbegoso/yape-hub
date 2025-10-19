package org.sky.dto.response.qr;

import java.time.LocalDateTime;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record ValidateAffiliationCodeResponse(
    Boolean isValid,
    String businessName,
    String branchName,
    LocalDateTime expiresAt
) {}
