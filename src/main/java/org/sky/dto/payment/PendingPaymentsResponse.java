package org.sky.dto.payment;

import java.util.List;

public record PendingPaymentsResponse(
    List<PaymentNotificationResponse> payments,
    PaginationInfo pagination
) {
    public record PaginationInfo(
        int currentPage,
        int totalPages,
        long totalItems,
        int itemsPerPage
    ) {}
}
