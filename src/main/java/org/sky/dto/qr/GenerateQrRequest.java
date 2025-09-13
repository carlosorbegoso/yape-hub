package org.sky.dto.qr;

import jakarta.validation.constraints.NotNull;
import org.sky.model.QrCode;

public record GenerateQrRequest(
    @NotNull(message = "QR type is required")
    QrCode.QrType type,
    
    Integer expirationHours,
    
    Integer maxUses,
    
    Long branchId
) {}
