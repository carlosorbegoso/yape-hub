package org.sky.dto.response.billing;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record PaymentUploadResponse(
    Long paymentId,
    String message,
    String status
) {}
