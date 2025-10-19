package org.sky.dto.response.seller;

import java.time.LocalDateTime;
import java.math.BigDecimal;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
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
