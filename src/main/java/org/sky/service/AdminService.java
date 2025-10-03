package org.sky.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import org.sky.dto.response.ApiResponse;
import org.sky.dto.response.admin.AdminProfileResponse;
import org.sky.dto.request.admin.UpdateAdminProfileRequest;
import org.sky.dto.response.common.BranchInfo;
import org.sky.repository.AdminRepository;
import org.sky.repository.BranchRepository;

import java.util.List;

@ApplicationScoped
public class AdminService {
    
    @Inject
    AdminRepository adminRepository;
    
    @Inject
    BranchRepository branchRepository;
    
    public Uni<ApiResponse<AdminProfileResponse>> getAdminProfile(Long userId) {
        return adminRepository.findByUserId(userId)
                .chain(admin -> {
                    if (admin == null) {
                        return Uni.createFrom().item(ApiResponse.<AdminProfileResponse>error("Admin not found"));
                    }
                    
                    return branchRepository.findByAdminId(admin.id)
                            .map(branches -> {
                                 List<BranchInfo> branchInfos = branches.stream()
                                         .map(branch -> new BranchInfo(
                                                 branch.id, branch.name, branch.address, null, branch.isActive, branch.createdAt))
                                         .toList();
                                
                                AdminProfileResponse response = new AdminProfileResponse(
                                        admin.id, admin.user.email, admin.businessName, admin.businessType,
                                        admin.ruc, admin.phone, admin.address, admin.contactName,
                                        admin.user.isVerified, admin.createdAt, branchInfos
                                );
                                
                                return ApiResponse.success("Admin profile retrieved successfully", response);
                            });
                });
    }
    
    @WithTransaction
    public Uni<ApiResponse<AdminProfileResponse>> updateAdminProfile(Long userId, UpdateAdminProfileRequest request) {
        return adminRepository.findByUserId(userId)
                .chain(admin -> {
                    if (admin == null) {
                        return Uni.createFrom().item(ApiResponse.<AdminProfileResponse>error("Admin not found"));
                    }
                    
                    // Update admin fields
                    if (request.businessName() != null) {
                        admin.businessName = request.businessName();
                    }
                    if (request.phone() != null) {
                        admin.phone = request.phone();
                    }
                    if (request.address() != null) {
                        admin.address = request.address();
                    }
                    if (request.contactName() != null) {
                        admin.contactName = request.contactName();
                    }
                    
                    return adminRepository.persist(admin)
                            .chain(persistedAdmin -> getAdminProfile(userId));
                });
    }
}
