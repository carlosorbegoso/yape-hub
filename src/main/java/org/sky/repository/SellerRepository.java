package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.Seller;

import java.util.List;

@ApplicationScoped
public class SellerRepository implements PanacheRepositoryBase<Seller, Long> {
    
    public Uni<Seller> findByUserId(Long userId) {
        return find("user.id", userId).firstResult();
    }
    
    public Uni<Seller> findByEmail(String email) {
        return find("email", email).firstResult();
    }
    
    public Uni<Seller> findByPhone(String phone) {
        return find("phone", phone).firstResult();
    }
    
    public Uni<List<Seller>> findByBranchId(Long branchId) {
        return find("branch.id", branchId).list();
    }
    
    public Uni<List<Seller>> findByAdminId(Long adminId) {
        return find("branch.admin.id", adminId).list();
    }
    
    public Uni<List<Seller>> findActiveByBranchId(Long branchId) {
        return find("branch.id = ?1 and isActive = true", branchId).list();
    }
    
    public Uni<List<Seller>> findActiveByAdminId(Long adminId) {
        return find("branch.admin.id = ?1 and isActive = true", adminId).list();
    }
    
    public Uni<List<Seller>> findOnlineSellers() {
        return find("isOnline = true").list();
    }
}
