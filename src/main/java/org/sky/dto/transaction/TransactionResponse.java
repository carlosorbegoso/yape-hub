package org.sky.dto.transaction;

import org.sky.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
    Long id,
    String securityCode,
    BigDecimal amount,
    LocalDateTime timestamp,
    String description,
    Transaction.TransactionType type,
    String businessName,
    Long branchId,
    String branchName,
    Long sellerId,
    String sellerName,
    Boolean isProcessed,
    Transaction.PaymentMethod paymentMethod,
    String customerPhone
) {}
