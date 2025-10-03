package org.sky.service.branch;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.response.ApiResponse;
import org.sky.dto.response.branch.BranchListResponse;
import org.sky.dto.response.branch.BranchResponse;
import org.sky.dto.response.common.PaginationInfo;
import org.sky.model.BranchEntity;
import org.sky.model.SellerEntity;
import org.sky.repository.SellerRepository;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class BranchResponseBuilder {

    @Inject
    SellerRepository sellerRepository;

    public BranchResponse buildBasicResponse(BranchEntity branch) {
        return new BranchResponse(
            branch.id,
            branch.name,
            branch.code,
            branch.address,
            branch.isActive,
            0L,
            branch.createdAt,
            branch.updatedAt
        );
    }

    public Uni<ApiResponse<BranchResponse>> buildResponseWithSellersCount(BranchEntity branch, String message) {
        return sellerRepository.countActiveByBranchId(branch.id)
                .map(sellersCount -> {
                    BranchResponse response = new BranchResponse(
                        branch.id,
                        branch.name,
                        branch.code,
                        branch.address,
                        branch.isActive,
                        sellersCount,
                        branch.createdAt,
                        branch.updatedAt
                    );
                    
                    return ApiResponse.success(message, response);
                });
    }

    public List<BranchResponse> buildListResponse(List<BranchEntity> branches) {
        return branches.stream()
                .map(this::buildBasicResponse)
                .toList();
    }

    public Map<String, Object> buildSellersResponse(BranchEntity branch, List<SellerEntity> sellers,
                                                   PaginationInfo paginationInfo) {
        BranchResponse branchInfo = new BranchResponse(
            branch.id,
            branch.name,
            branch.code,
            branch.address,
            branch.isActive,
            (long) sellers.size(),
            branch.createdAt,
            branch.updatedAt
        );
        
        return Map.of(
            "branch", branchInfo,
            "sellers", sellers.stream()
                .map(seller -> Map.of(
                    "sellerId", seller.id,
                    "sellerName", seller.sellerName,
                    "email", seller.email,
                    "phone", seller.phone,
                    "isActive", seller.isActive,
                    "createdAt", seller.createdAt
                ))
                .toList(),
            "pagination", paginationInfo
        );
    }
}
