package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.QrCode;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class QrCodeRepository implements PanacheRepositoryBase<QrCode, Long> {
    

    public Uni<List<QrCode>> findActiveByBranchId(Long branchId) {
        return find("branch.id = ?1 and isActive = true and (expiresAt is null or expiresAt > ?2)", 
                   branchId, LocalDateTime.now()).list();
    }

}
