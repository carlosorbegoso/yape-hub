package org.sky.dto.response.seller;

import org.sky.dto.response.common.PaginationInfo;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record SellerListResponse(
    List<SellerResponse> sellers,
    PaginationInfo pagination
) {}
