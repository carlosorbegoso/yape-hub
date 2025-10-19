package org.sky.dto.response.stats;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record TopSellerData(
    Integer rank,

    Long sellerId,


    String sellerName,


    String branchName,


    Double totalSales,


    Long transactionCount
) {}
