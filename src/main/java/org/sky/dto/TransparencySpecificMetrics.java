package org.sky.dto;

public record TransparencySpecificMetrics(
    Double processingFees,
    Double platformFees,
    Double taxRate,
    Double taxAmount,
    Double sellerCommissionRate,
    Double sellerCommissionAmount,
    Double transparencyScore
) {}