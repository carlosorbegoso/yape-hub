package org.sky.dto.response.stats;

public record TopSellerData(
    Integer rank,

    Long sellerId,


    String sellerName,


    String branchName,


    Double totalSales,


    Long transactionCount
) {}