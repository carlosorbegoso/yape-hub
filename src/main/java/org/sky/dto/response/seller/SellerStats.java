package org.sky.dto.response.seller;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record SellerStats(
    Long sellerId,
    String sellerName,
    Double totalSales,
    Long transactionCount,
    Double averageValue,
    Long pendingCount
) {}
