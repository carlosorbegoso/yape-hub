package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.AdminSubscriptionEntity;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class AdminSubscriptionRepository implements PanacheRepository<AdminSubscriptionEntity> {

    public Uni<AdminSubscriptionEntity> findActiveByAdminId(Long adminId) {
        return find("adminId = ?1 and status = 'active' and (endDate is null or endDate > ?2)", 
                   adminId, LocalDateTime.now())
                .firstResult();
    }

    public Uni<List<AdminSubscriptionEntity>> findByAdminId(Long adminId) {
        return find("adminId = ?1 order by startDate desc", adminId).list();
    }

}