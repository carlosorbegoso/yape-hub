package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.SubscriptionPlan;

import java.util.List;

@ApplicationScoped
public class SubscriptionPlanRepository implements PanacheRepository<SubscriptionPlan> {



    public Uni<SubscriptionPlan> findByName(String name) {
        return find("name = ?1", name).firstResult();
    }

}