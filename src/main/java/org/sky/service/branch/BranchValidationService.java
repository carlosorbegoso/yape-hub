package org.sky.service.branch;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.exception.ValidationException;
import org.sky.model.AdminEntity;
import org.sky.model.BusinessType;
import org.sky.model.BranchEntity;
import org.sky.repository.AdminRepository;
import org.sky.repository.BranchRepository;

@ApplicationScoped
public class BranchValidationService {

    @Inject
    AdminRepository adminRepository;
    
    @Inject
    BranchRepository branchRepository;

    public Uni<AdminEntity> validateAdminExists(Long adminId) {
        return adminRepository.findById(adminId)
                .chain(admin -> {
                    if (admin == null) {
                        return Uni.createFrom().failure(
                            ValidationException.invalidField("adminId", adminId.toString(), "Administrator not found")
                        );
                    }
                    return Uni.createFrom().item(admin);
                });
    }

    public Uni<BranchEntity> validateBranchExists(Long adminId, Long branchId) {
        return branchRepository.findByAdminIdAndBranchId(adminId, branchId)
                .chain(branch -> {
                    if (branch == null) {
                        return Uni.createFrom().failure(
                            ValidationException.invalidField("branchId", branchId.toString(), "Branch not found")
                        );
                    }
                    return Uni.createFrom().item(branch);
                });
    }

    public Uni<Void> validateCodeNotExists(String code) {
        return branchRepository.findByCode(code)
                .chain(existingBranch -> {
                    if (existingBranch != null) {
                        return Uni.createFrom().failure(
                            ValidationException.invalidField("code", code, "Branch code already exists")
                        );
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    public Uni<Void> validateCodeNotExistsExcludingId(String code, Long branchId) {
        return branchRepository.findByCodeExcludingId(code, branchId)
                .chain(existingBranch -> {
                    if (existingBranch != null) {
                        return Uni.createFrom().failure(
                            ValidationException.invalidField("code", code, "Branch code already exists")
                        );
                    }
                    return Uni.createFrom().voidItem();
                });
    }
}
