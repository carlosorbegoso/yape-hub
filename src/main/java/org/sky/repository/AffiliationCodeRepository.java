package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.AffiliationCode;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class AffiliationCodeRepository implements PanacheRepositoryBase<AffiliationCode, Long> {
    
    public Uni<AffiliationCode> findByAffiliationCode(String affiliationCode) {
        return find("affiliationCode", affiliationCode).firstResult();
    }
    
    public Uni<List<AffiliationCode>> findByBranchId(Long branchId) {
        return find("branch.id", branchId).list();
    }
    
    public Uni<List<AffiliationCode>> findActiveByBranchId(Long branchId) {
        return find("branch.id = ?1 and isActive = true and (expiresAt is null or expiresAt > ?2)", 
                   branchId, LocalDateTime.now()).list();
    }
    
    public Uni<List<AffiliationCode>> findActiveCodes() {
        return find("isActive = true and (expiresAt is null or expiresAt > ?1)", LocalDateTime.now()).list();
    }
    
    public Uni<List<AffiliationCode>> findExpiredCodes() {
        return find("expiresAt is not null and expiresAt < ?1", LocalDateTime.now()).list();
    }
    
    public Uni<List<AffiliationCode>> findAvailableCodes() {
        return find("isActive = true and remainingUses > 0 and (expiresAt is null or expiresAt > ?1)", 
                   LocalDateTime.now()).list();
    }
    
    public Uni<List<AffiliationCode>> findExhaustedCodes() {
        return find("remainingUses <= 0").list();
    }
}
