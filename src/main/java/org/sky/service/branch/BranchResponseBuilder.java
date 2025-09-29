package org.sky.service.branch;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.ApiResponse;
import org.sky.dto.branch.BranchListResponse;
import org.sky.dto.branch.BranchResponse;
import org.sky.model.Branch;
import org.sky.model.Seller;
import org.sky.repository.SellerRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class BranchResponseBuilder {

    @Inject
    SellerRepository sellerRepository;

    public BranchResponse buildBasicResponse(Branch branch) {
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

    public Uni<ApiResponse<BranchResponse>> buildResponseWithSellersCount(Branch branch, String message) {
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

    public List<BranchResponse> buildListResponse(List<Branch> branches) {
        return branches.stream()
                .map(this::buildBasicResponse)
                .toList();
    }

    public Map<String, Object> buildSellersResponse(Branch branch, List<Seller> sellers,
                                                   BranchListResponse.PaginationInfo paginationInfo) {
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
