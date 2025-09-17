package org.sky.dto.qr;

import com.fasterxml.jackson.annotation.JsonProperty;

public record QrBase64Response(
    @JsonProperty("affiliationCode")
    String affiliationCode,

    @JsonProperty("qrBase64")
    String qrBase64,

    @JsonProperty("expiresAt")
    String expiresAt,

    @JsonProperty("maxUses")
    Integer maxUses,

    @JsonProperty("remainingUses")
    Integer remainingUses,

    @JsonProperty("branchName")
    String branchName,

    @JsonProperty("adminName")
    String adminName
) {}
