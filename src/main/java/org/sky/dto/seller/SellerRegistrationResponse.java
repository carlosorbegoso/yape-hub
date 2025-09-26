package org.sky.dto.seller;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record SellerRegistrationResponse(
    Long sellerId,
    String name,
    String email,
    String phone,
    Long branchId,
    String branchName,
    Boolean isActive,
    Boolean isOnline,
    Integer totalPayments,
    BigDecimal totalAmount,
    LocalDateTime lastPayment,
    LocalDateTime affiliationDate,
    String token  // Token JWT para autenticación inmediata
) {}
