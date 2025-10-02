package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.SellerEntity;

import java.util.List;

@ApplicationScoped
public class SellerRepository implements PanacheRepository<SellerEntity> {

    public Uni<SellerEntity> findByPhone(String phone) {
        return find("phone", phone).firstResult();
    }

    public Uni<List<SellerEntity>> findByBranchId(Long branchId) {
        return find("branch.id = ?1 ORDER BY id", branchId).range(0, 50).list(); // Limited to 50 for low-resource efficiency
    }

    public Uni<List<SellerEntity>> findByAdminId(Long adminId) {
        return find("SELECT s FROM SellerEntity s JOIN FETCH s.branch b WHERE b.admin.id = ?1 ORDER BY s.id", adminId).range(0, 50).list(); // Limited to 50 for low-resource efficiency
    }
    
    public Uni<List<SellerEntity>> findActiveSellersByAdminId(Long adminId) {
        return find("branch.admin.id = ?1 and isActive = true", adminId)
                .page(0, 100)  // LÍMITE: máximo 100 sellers
                .list();
    }

    public Uni<SellerEntity> findByUserId(Long userId) {
        return find("user.id", userId).firstResult();
    }

    public Uni<SellerEntity> findBySellerIdAndAdminId(Long sellerId, Long adminId) {
        return find("id = ?1 and branch.admin.id = ?2", sellerId, adminId).firstResult();
    }
    
    public Uni<Long> countActiveByBranchId(Long branchId) {
        return count("branch.id = ?1 and isActive = true", branchId);
    }
    
    public Uni<SellerPaginationResult> findByBranchIdWithPagination(Long branchId, int page, int size) {
        return find("branch.id = ?1", branchId)
                .page(page, size)
                .list()
                .chain(sellers -> 
                    count("branch.id = ?1", branchId)
                            .map(totalCount -> new SellerPaginationResult(sellers, totalCount))
                );
    }
    
    public record SellerPaginationResult(List<SellerEntity> sellers, Long totalCount) {}
}
