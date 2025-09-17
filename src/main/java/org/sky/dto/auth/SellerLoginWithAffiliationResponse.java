package org.sky.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SellerLoginWithAffiliationResponse(
    @JsonProperty("sellerId")
    Long sellerId,

    @JsonProperty("sellerName")
    String sellerName,

    @JsonProperty("email")
    String email,

    @JsonProperty("phone")
    String phone,

    @JsonProperty("branchId")
    Long branchId,

    @JsonProperty("branchName")
    String branchName,

    @JsonProperty("branchCode")
    String branchCode,

    @JsonProperty("affiliationCode")
    String affiliationCode,

    @JsonProperty("accessToken")
    String accessToken
) {}
