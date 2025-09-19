package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.YapeNotificationAudit;

@ApplicationScoped
public class YapeNotificationAuditRepository implements PanacheRepository<YapeNotificationAudit> {
    
    /**
     * Busca una notificación de auditoría por su hash de deduplicación
     */
    public io.smallrye.mutiny.Uni<YapeNotificationAudit> findByDeduplicationHash(String deduplicationHash) {
        return find("deduplicationHash = ?1", deduplicationHash).firstResult();
    }
    
    /**
     * Busca notificaciones de auditoría por admin
     */
    public io.smallrye.mutiny.Uni<java.util.List<YapeNotificationAudit>> findByAdminId(Long adminId) {
        return find("adminId = ?1 order by createdAt desc", adminId).list();
    }
    
    /**
     * Busca notificaciones de auditoría por estado de desencriptación
     */
    public io.smallrye.mutiny.Uni<java.util.List<YapeNotificationAudit>> findByDecryptionStatus(String status) {
        return find("decryptionStatus = ?1 order by createdAt desc", status).list();
    }
}
