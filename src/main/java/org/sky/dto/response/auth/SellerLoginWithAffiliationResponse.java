package org.sky.dto.response.auth;

public record SellerLoginWithAffiliationResponse(
    Long sellerId,
    String sellerName,
    String email,
    String phone,
    Long branchId,
    String branchName,
    String branchCode,
    String affiliationCode,
    String accessToken
) {}