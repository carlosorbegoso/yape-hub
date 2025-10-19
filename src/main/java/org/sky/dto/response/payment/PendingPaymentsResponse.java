package org.sky.dto.response.payment;

import org.sky.dto.response.common.PaginationInfo;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record PendingPaymentsResponse(
    List<PaymentNotificationResponse> payments,
    PaginationInfo pagination
) {}
