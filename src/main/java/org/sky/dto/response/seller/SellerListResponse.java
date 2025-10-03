package org.sky.dto.response.seller;

import org.sky.dto.response.common.PaginationInfo;

import java.util.List;

public record SellerListResponse(
    List<SellerResponse> sellers,
    PaginationInfo pagination
) {}
