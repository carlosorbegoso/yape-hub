package org.sky.dto.response.payment;

import java.time.LocalDateTime;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record PaymentDetail(
    Long paymentId,
    Double amount,
    String senderName,
    String yapeCode,
    String status,
    LocalDateTime createdAt,
    Long confirmedBy,
    LocalDateTime confirmedAt,
    Long rejectedBy,
    LocalDateTime rejectedAt,
    String rejectionReason,
    String sellerName,
    String branchName
) {}
