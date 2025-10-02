package org.sky.dto.stats;

public record TopSellerData(
    Integer rank,

    Long sellerId,


    String sellerName,


    String branchName,


    Double totalSales,


    Long transactionCount
) {}