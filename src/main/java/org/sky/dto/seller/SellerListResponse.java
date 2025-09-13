package org.sky.dto.seller;

import java.util.List;

public record SellerListResponse(
    List<SellerResponse> sellers,
    PaginationInfo pagination
) {
    public record PaginationInfo(
        int currentPage,
        int totalPages,
        long totalItems,
        int itemsPerPage
    ) {}
}
