package org.sky.dto.seller;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record SellerProfileResponse(
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

    @JsonProperty("isActive")
    Boolean isActive,

    @JsonProperty("isOnline")
    Boolean isOnline,

    @JsonProperty("totalPayments")
    Integer totalPayments,

    @JsonProperty("totalAmount")
    Double totalAmount,

    @JsonProperty("lastPayment")
    LocalDateTime lastPayment,

    @JsonProperty("affiliationDate")
    LocalDateTime affiliationDate,

    @JsonProperty("connectionStatus")
    String connectionStatus,

    @JsonProperty("lastSeen")
    LocalDateTime lastSeen
) {}
