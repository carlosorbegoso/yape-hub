package org.sky.dto.request.qr;

import jakarta.validation.constraints.NotBlank;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record QrLoginRequest(
    @NotBlank(message = "QR data is required")
    String qrData,

    @NotBlank(message = "Phone is required")
    String phone
) {}
