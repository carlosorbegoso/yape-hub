package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.Branch;

import java.util.List;

@ApplicationScoped
public class BranchRepository implements PanacheRepositoryBase<Branch, Long> {
    
    public Uni<List<Branch>> findByAdminId(Long adminId) {
        return find("admin.id", adminId).list();
    }
    
    public Uni<Branch> findByCode(String code) {
        return find("code", code).firstResult();
    }
    
    public Uni<List<Branch>> findActiveByAdminId(Long adminId) {
        return find("admin.id = ?1 and isActive = true", adminId).list();
    }
    
    public Uni<List<Branch>> findActiveBranches() {
        return find("isActive = true").list();
    }
}
