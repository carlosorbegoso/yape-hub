package org.sky.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record TransparencySpecificMetrics(
    Double processingFees,
    Double platformFees,
    Double taxRate,
    Double taxAmount,
    Double sellerCommissionRate,
    Double sellerCommissionAmount,
    Double transparencyScore
) {}
