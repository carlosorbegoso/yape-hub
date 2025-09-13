package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.Transaction;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class TransactionRepository implements PanacheRepositoryBase<Transaction, Long> {
    
    public Uni<Transaction> findBySecurityCode(String securityCode) {
        return find("securityCode", securityCode).firstResult();
    }
    
    public Uni<List<Transaction>> findByBranchId(Long branchId) {
        return find("branch.id", branchId).list();
    }
    
    public Uni<List<Transaction>> findBySellerId(Long sellerId) {
        return find("seller.id", sellerId).list();
    }
    
    public Uni<List<Transaction>> findByAdminId(Long adminId) {
        return find("branch.admin.id", adminId).list();
    }
    
    public Uni<List<Transaction>> findPendingTransactions() {
        return find("isProcessed = false").list();
    }
    
    public Uni<List<Transaction>> findConfirmedTransactions() {
        return find("isConfirmed = true").list();
    }
    
    public Uni<List<Transaction>> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return find("timestamp between ?1 and ?2", startDate, endDate).list();
    }
    
    public Uni<List<Transaction>> findByBranchAndDateRange(Long branchId, LocalDateTime startDate, LocalDateTime endDate) {
        return find("branch.id = ?1 and timestamp between ?2 and ?3", branchId, startDate, endDate).list();
    }
    
    public Uni<List<Transaction>> findBySellerAndDateRange(Long sellerId, LocalDateTime startDate, LocalDateTime endDate) {
        return find("seller.id = ?1 and timestamp between ?2 and ?3", sellerId, startDate, endDate).list();
    }
    
    public Uni<List<Transaction>> findByAdminAndDateRange(Long adminId, LocalDateTime startDate, LocalDateTime endDate) {
        return find("branch.admin.id = ?1 and timestamp between ?2 and ?3", adminId, startDate, endDate).list();
    }
}
