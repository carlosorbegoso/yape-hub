package org.sky.service.branch;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import org.sky.dto.response.ApiResponse;
import org.sky.dto.request.branch.BranchCreateRequest;
import org.sky.dto.request.branch.BranchUpdateRequest;
import org.sky.dto.response.branch.BranchResponse;
import org.sky.dto.response.branch.BranchListResponse;
import org.sky.dto.response.common.PaginationInfo;
import org.sky.model.BranchEntity;
import org.sky.repository.BranchRepository;
import org.sky.repository.SellerRepository;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class BranchService {
    
    @Inject
    BranchRepository branchRepository;
    
    @Inject
    SellerRepository sellerRepository;
    
    @Inject
    BranchValidationService validationService;
    
    @Inject
    BranchResponseBuilder responseBuilder;

    @WithTransaction
    public Uni<ApiResponse<BranchResponse>> createBranch(Long adminId, BranchCreateRequest request) {
        return validationService.validateAdminExists(adminId)
                .chain(admin -> validationService.validateCodeNotExists(request.code())
                        .chain(v -> {
                            BranchEntity branch = new BranchEntity();
                            branch.admin = admin;
                            branch.name = request.name();
                            branch.code = request.code();
                            branch.address = request.address();
                            branch.isActive = true;
                            
                            return branchRepository.persist(branch)
                                    .map(savedBranch -> {
                                        BranchResponse response = responseBuilder.buildBasicResponse(savedBranch);
                                        return ApiResponse.success("Branch created successfully", response);
                                    });
                        }));
    }

    @WithTransaction
    public Uni<ApiResponse<BranchListResponse>> listBranches(Long adminId, int page, int size, String status) {
        final int validatedPage = Math.max(0, page);
        final int validatedSize = (size <= 0 || size > 100) ? 20 : size;
        
        return branchRepository.findByAdminIdWithPagination(adminId, status, validatedPage, validatedSize)
                .map(result -> {
                    List<BranchResponse> branchResponses = responseBuilder.buildListResponse(result.branches());
                    
                    int totalPages = (int) Math.ceil((double) result.totalCount() / validatedSize);
                    
                    PaginationInfo paginationInfo =
                        new PaginationInfo(
                            validatedPage,
                            totalPages,
                            result.totalCount(),
                            validatedSize,
                            validatedPage < totalPages,
                            validatedPage > 1
                        );
                    
                    BranchListResponse response = new BranchListResponse(branchResponses, paginationInfo);
                    return ApiResponse.success("Branches retrieved successfully", response);
                });
    }

    @WithTransaction
    public Uni<ApiResponse<BranchResponse>> getBranchById(Long adminId, Long branchId) {
        return validationService.validateBranchExists(adminId, branchId)
                .chain(branch -> responseBuilder.buildResponseWithSellersCount(branch, 
                    "Branch details retrieved successfully"));
    }

    @WithTransaction
    public Uni<ApiResponse<BranchResponse>> updateBranch(Long adminId, Long branchId, BranchUpdateRequest request) {
        return validationService.validateBranchExists(adminId, branchId)
                .chain(branch -> {
                    if (request.code() != null && !request.code().equals(branch.code)) {
                        return validationService.validateCodeNotExistsExcludingId(request.code(), branchId)
                                .chain(v -> updateAndPersistBranch(branch, request));
                    } else {
                        return updateAndPersistBranch(branch, request);
                    }
                });
    }

    @WithTransaction
    public Uni<ApiResponse<String>> deleteBranch(Long adminId, Long branchId) {
        return validationService.validateBranchExists(adminId, branchId)
                .chain(branch -> sellerRepository.countActiveByBranchId(branchId)
                        .chain(sellersCount -> {
                            if (sellersCount > 0) {
                                return Uni.createFrom().failure(
                                    org.sky.exception.ValidationException.invalidField("branchId", branchId.toString(), 
                                        "Cannot delete branch because it has active sellers")
                                );
                            }
                            
                            branch.isActive = false;
                            return branchRepository.persist(branch)
                                    .map(updatedBranch -> ApiResponse.success("Branch deleted successfully", 
                                        "The branch has been marked as inactive"));
                        }));
    }

    @WithTransaction
    public Uni<ApiResponse<Map<String, Object>>> getBranchSellers(Long adminId, Long branchId, int page, int size) {
        return validationService.validateBranchExists(adminId, branchId)
                .chain(branch -> {
                    final int validatedPage = Math.max(0, page);
                    final int validatedSize = (size <= 0 || size > 100) ? 20 : size;
                    
                    return sellerRepository.findByBranchIdWithPagination(branchId, validatedPage, validatedSize)
                            .map(result -> {
                                PaginationInfo paginationInfo = getPaginationInfo(result, validatedSize, validatedPage);
                                
                                Map<String, Object> responseData = responseBuilder.buildSellersResponse(
                                    branch, result.sellers(), paginationInfo);
                                
                                return ApiResponse.success("Branch sellers retrieved successfully", responseData);
                            });
                });
    }

    private static PaginationInfo getPaginationInfo(SellerRepository.SellerPaginationResult result, int validatedSize, int validatedPage) {
        int totalPages = (int) Math.ceil((double) result.totalCount() / validatedSize);

      return new PaginationInfo(
          validatedPage,
          totalPages,
          result.totalCount(),
          validatedSize,
          validatedPage < totalPages,
          validatedPage > 1
      );
    }

    private Uni<ApiResponse<BranchResponse>> updateAndPersistBranch(BranchEntity branch, BranchUpdateRequest request) {
        updateBranchFields(branch, request);
        
        return branchRepository.persist(branch)
                .chain(updatedBranch -> responseBuilder.buildResponseWithSellersCount(updatedBranch, 
                    "Branch updated successfully"));
    }

    private void updateBranchFields(BranchEntity branch, BranchUpdateRequest request) {
        if (request.name() != null) branch.name = request.name();
        if (request.code() != null) branch.code = request.code();
        if (request.address() != null) branch.address = request.address();
        if (request.isActive() != null) branch.isActive = request.isActive();
    }
}
