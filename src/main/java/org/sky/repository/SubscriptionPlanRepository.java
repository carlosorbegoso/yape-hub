package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.SubscriptionPlanEntity;

@ApplicationScoped
public class SubscriptionPlanRepository implements PanacheRepository<SubscriptionPlanEntity> {



    public Uni<SubscriptionPlanEntity> findByName(String name) {
        return find("name = ?1", name).firstResult();
    }

}