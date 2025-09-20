package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.AdminSubscription;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class AdminSubscriptionRepository implements PanacheRepository<AdminSubscription> {

    public Uni<AdminSubscription> findActiveByAdminId(Long adminId) {
        return find("adminId = ?1 and status = 'active' and (endDate is null or endDate > ?2)", 
                   adminId, LocalDateTime.now())
                .firstResult();
    }

    public Uni<List<AdminSubscription>> findByAdminId(Long adminId) {
        return find("adminId = ?1 order by startDate desc", adminId).list();
    }

    public Uni<List<AdminSubscription>> findActiveSubscriptions() {
        return find("status = 'active' and (endDate is null or endDate > ?1)", LocalDateTime.now()).list();
    }

    public Uni<Long> countActiveSubscriptions() {
        return count("status = 'active' and (endDate is null or endDate > ?1)", LocalDateTime.now());
    }

    public Uni<List<AdminSubscription>> findExpiredSubscriptions() {
        return find("status = 'active' and endDate <= ?1", LocalDateTime.now()).list();
    }

    public Uni<List<AdminSubscription>> findByPlanId(Long planId) {
        return find("planId = ?1", planId).list();
    }
}