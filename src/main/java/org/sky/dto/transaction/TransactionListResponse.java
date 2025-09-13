package org.sky.dto.transaction;

import java.util.List;

public record TransactionListResponse(
    List<TransactionResponse> transactions,
    PaginationInfo pagination
) {
    public record PaginationInfo(
        int currentPage,
        int totalPages,
        long totalItems,
        int itemsPerPage
    ) {}
}
