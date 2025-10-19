package org.sky.dto.response.payment;

import org.sky.dto.response.common.Period;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record PaymentTransparencyResponse(
    Period period,
    Double totalSales,
    Long totalTransactions,
    Long confirmedTransactions,
    Double processingFees,
    Double platformFees,
    Double taxRate,
    Double taxAmount,
    Double sellerCommissionRate,
    Double sellerCommissionAmount,
    Double transparencyScore,
    String generatedAt
) {}
