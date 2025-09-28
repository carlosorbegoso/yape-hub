package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.YapeNotificationAudit;

@ApplicationScoped
public class YapeNotificationAuditRepository implements PanacheRepository<YapeNotificationAudit> {
    


    public io.smallrye.mutiny.Uni<java.util.List<YapeNotificationAudit>> findByAdminId(Long adminId) {
        return find("adminId = ?1 order by createdAt desc", adminId).list();
    }
    

}
