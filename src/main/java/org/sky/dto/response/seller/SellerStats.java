package org.sky.dto.response.seller;

public record SellerStats(
    Long sellerId,
    String sellerName,
    Double totalSales,
    Long transactionCount,
    Double averageValue,
    Long pendingCount
) {}
