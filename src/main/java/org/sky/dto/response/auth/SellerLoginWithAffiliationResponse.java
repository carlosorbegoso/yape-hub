package org.sky.dto.response.auth;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
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