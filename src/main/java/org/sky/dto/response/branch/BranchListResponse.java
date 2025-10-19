package org.sky.dto.response.branch;

import org.sky.dto.response.common.PaginationInfo;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record BranchListResponse(
    List<BranchResponse> branches,
    PaginationInfo pagination
) {}
