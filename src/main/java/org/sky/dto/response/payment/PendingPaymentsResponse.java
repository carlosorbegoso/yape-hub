package org.sky.dto.response.payment;

import org.sky.dto.response.common.PaginationInfo;

import java.util.List;

public record PendingPaymentsResponse(
    List<PaymentNotificationResponse> payments,
    PaginationInfo pagination
) {}
