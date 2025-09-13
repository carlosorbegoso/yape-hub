package org.sky.dto.seller;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SellerResponse(
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
    LocalDateTime affiliationDate
) {}
