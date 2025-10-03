package org.sky.dto.response.billing;

public record PaymentUploadResponse(
    Long paymentId,
    String message,
    String status
) {}
