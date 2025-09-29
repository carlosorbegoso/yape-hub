package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.Branch;

import java.util.List;

@ApplicationScoped
public class BranchRepository implements PanacheRepository<Branch> {
    
    public Uni<List<Branch>> findByAdminId(Long adminId) {
        return find("admin.id", adminId).list();
    }
    
    public Uni<Branch> findByCode(String code) {
        return find("code", code).firstResult();
    }
    
    public Uni<Branch> findByAdminIdAndBranchId(Long adminId, Long branchId) {
        return find("id = ?1 and admin.id = ?2", branchId, adminId).firstResult();
    }
    
    public Uni<Branch> findByCodeExcludingId(String code, Long branchId) {
        return find("code = ?1 and id != ?2", code, branchId).firstResult();
    }
    
    public Uni<BranchPaginationResult> findByAdminIdWithPagination(Long adminId, String status, int page, int size) {
        String query;
        List<Object> params;
        
        if ("active".equals(status)) {
            query = "admin.id = ?1 and isActive = true";
            params = List.of(adminId);
        } else if ("inactive".equals(status)) {
            query = "admin.id = ?1 and isActive = false";
            params = List.of(adminId);
        } else {
            query = "admin.id = ?1";
            params = List.of(adminId);
        }
        
        return find(query, params.toArray())
                .page(page, size)
                .list()
                .chain(branches -> 
                    count(query, params.toArray())
                            .map(totalCount -> new BranchPaginationResult(branches, totalCount))
                );
    }
    
    public record BranchPaginationResult(List<Branch> branches, Long totalCount) {}
}
