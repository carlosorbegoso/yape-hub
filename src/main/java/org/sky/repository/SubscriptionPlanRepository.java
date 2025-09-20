package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.SubscriptionPlan;

import java.util.List;

@ApplicationScoped
public class SubscriptionPlanRepository implements PanacheRepository<SubscriptionPlan> {

    public Uni<List<SubscriptionPlan>> findActivePlans() {
        return find("isActive = true order by pricePen asc").list();
    }

    public Uni<List<SubscriptionPlan>> findByBillingCycle(String billingCycle) {
        return find("billingCycle = ?1 and isActive = true", billingCycle).list();
    }

    public Uni<SubscriptionPlan> findByName(String name) {
        return find("name = ?1", name).firstResult();
    }

    public Uni<Long> countActivePlans() {
        return count("isActive = true");
    }

    public Uni<List<SubscriptionPlan>> findPlansByPriceRange(Double minPrice, Double maxPrice) {
        return find("pricePen >= ?1 and pricePen <= ?2 and isActive = true", minPrice, maxPrice).list();
    }
}