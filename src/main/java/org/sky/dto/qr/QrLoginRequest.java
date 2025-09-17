package org.sky.dto.qr;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record QrLoginRequest(
    @JsonProperty("qrData")
    @NotBlank(message = "QR data is required")
    String qrData,

    @JsonProperty("phone")
    @NotBlank(message = "Phone is required")
    String phone
) {}
