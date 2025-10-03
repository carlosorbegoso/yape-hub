package org.sky.service.billing;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.ApiResponse;
import org.sky.model.SubscriptionPlanEntity;
import org.sky.repository.SubscriptionPlanRepository;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class BillingPlansService {

    @Inject
    SubscriptionPlanRepository subscriptionPlanRepository;
    
    
    @Inject
    BillingResponseBuilder responseBuilder;

    public Uni<ApiResponse<List<Map<String, Object>>>> getAvailablePlans(String include) {
        return subscriptionPlanRepository.findAll()
                .list()
                .map(plans -> responseBuilder.buildPlansResponse(plans));
    }


    @WithTransaction
    public Uni<ApiResponse<Map<String, Object>>> loadData() {
        SubscriptionPlanEntity planGratuito = createPlan("Plan Gratuito", "Plan básico gratuito con funcionalidades limitadas", 0.00, "monthly", 1, 1, "[\"QR Generation\", \"Payment Processing\", \"Basic Analytics\"]");
        SubscriptionPlanEntity planBasico = createPlan("Basic Plan", "Basic plan with essential features for small businesses", 50.00, "monthly", 1, 2, "[\"QR Generation\", \"Payment Processing\", \"Basic Analytics\", \"Email Support\"]");
        SubscriptionPlanEntity planProfesional = createPlan("Professional Plan", "Professional plan with advanced features for growing businesses", 150.00, "monthly", 3, 6, "[\"QR Generation\", \"Payment Processing\", \"Advanced Analytics\", \"Priority Support\", \"API Access\"]");
        SubscriptionPlanEntity planEmpresarial = createPlan("Enterprise Plan", "Enterprise plan with complete features for large businesses", 300.00, "monthly", 10, 6, "[\"QR Generation\", \"Payment Processing\", \"Advanced Analytics\", \"Priority Support\", \"API Access\", \"Custom Integrations\", \"Dedicated Support\"]");
        
        return subscriptionPlanRepository.persist(planGratuito)
                .chain(savedPlan0 -> subscriptionPlanRepository.persist(planBasico))
                .chain(savedPlan1 -> subscriptionPlanRepository.persist(planProfesional))
                .chain(savedPlan2 -> subscriptionPlanRepository.persist(planEmpresarial))
                .map(savedPlan3 -> ApiResponse.success("Plans created successfully", Map.of(
                        "plansCreated", 4,
                        "message", "Free, Basic, Professional and Enterprise plans created"
                )));
    }

    private SubscriptionPlanEntity createPlan(String name, String description, double price, String billingCycle,
                                              int maxAdmins, int maxSellers, String features) {
        SubscriptionPlanEntity plan = new SubscriptionPlanEntity();
        plan.name = name;
        plan.description = description;
        plan.pricePen = java.math.BigDecimal.valueOf(price);
        plan.billingCycle = billingCycle;
        plan.maxAdmins = maxAdmins;
        plan.maxSellers = maxSellers;
        plan.features = features;
        plan.isActive = true;
        return plan;
    }
}
