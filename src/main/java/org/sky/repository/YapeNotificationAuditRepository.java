package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.YapeNotificationAuditEntity;

@ApplicationScoped
public class YapeNotificationAuditRepository implements PanacheRepository<YapeNotificationAuditEntity> {
    


    public io.smallrye.mutiny.Uni<java.util.List<YapeNotificationAuditEntity>> findByAdminId(Long adminId) {
        return find("adminId = ?1 order by createdAt desc", adminId).range(0, 100).list(); // LIMIT 100
    }
    
    public io.smallrye.mutiny.Uni<java.util.List<YapeNotificationAuditEntity>> findByAdminIdWithLimit(Long adminId, int limit) {
        return find("adminId = ?1 order by createdAt desc", adminId).range(0, Math.min(limit, 1000)).list(); // MAX 1000
    }
    

}
