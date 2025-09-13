package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.QrCode;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class QrCodeRepository implements PanacheRepositoryBase<QrCode, Long> {
    
    public Uni<QrCode> findByQrData(String qrData) {
        return find("qrData", qrData).firstResult();
    }
    
    public Uni<List<QrCode>> findByBranchId(Long branchId) {
        return find("branch.id", branchId).list();
    }
    
    public Uni<List<QrCode>> findActiveByBranchId(Long branchId) {
        return find("branch.id = ?1 and isActive = true and (expiresAt is null or expiresAt > ?2)", 
                   branchId, LocalDateTime.now()).list();
    }
    
    public Uni<List<QrCode>> findByType(QrCode.QrType type) {
        return find("type", type).list();
    }
    
    public Uni<List<QrCode>> findActiveByType(QrCode.QrType type) {
        return find("type = ?1 and isActive = true and (expiresAt is null or expiresAt > ?2)", 
                   type, LocalDateTime.now()).list();
    }
    
    public Uni<List<QrCode>> findExpiredCodes() {
        return find("expiresAt is not null and expiresAt < ?1", LocalDateTime.now()).list();
    }
    
    public Uni<List<QrCode>> findActiveCodes() {
        return find("isActive = true and (expiresAt is null or expiresAt > ?1)", LocalDateTime.now()).list();
    }
}
