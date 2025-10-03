package org.sky.service.billing;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.ApiResponse;
import org.sky.model.SubscriptionPlanEntity;
import org.sky.repository.SubscriptionPlanRepository;
import org.sky.repository.TokenPackageRepository;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class BillingPlansService {

    @Inject
    SubscriptionPlanRepository subscriptionPlanRepository;
    
    @Inject
    TokenPackageRepository tokenPackageRepository;
    
    @Inject
    BillingResponseBuilder responseBuilder;

    public Uni<ApiResponse<List<Map<String, Object>>>> getAvailablePlans(String include) {
        return subscriptionPlanRepository.findAll()
                .list()
                .map(plans -> responseBuilder.buildPlansResponse(plans));
    }

    public Uni<ApiResponse<List<Map<String, Object>>>> getTokenPackages(String include) {
        return tokenPackageRepository.findActivePackages()
                .map(packages -> responseBuilder.buildTokenPackagesResponse(packages));
    }

    @WithTransaction
    public Uni<ApiResponse<Map<String, Object>>> loadData() {
        SubscriptionPlanEntity planBasico = createPlan("Basic Plan", "Basic plan with essential features for small businesses", 50.00, "monthly", 1, 5, 500, "[\"QR Generation\", \"Payment Processing\", \"Basic Analytics\", \"Email Support\"]");
        SubscriptionPlanEntity planProfesional = createPlan("Professional Plan", "Professional plan with advanced features for growing businesses", 150.00, "monthly", 3, 20, 2000, "[\"QR Generation\", \"Payment Processing\", \"Advanced Analytics\", \"Priority Support\", \"API Access\"]");
        SubscriptionPlanEntity planEmpresarial = createPlan("Enterprise Plan", "Enterprise plan with complete features for large businesses", 300.00, "monthly", 10, 100, 10000, "[\"QR Generation\", \"Payment Processing\", \"Advanced Analytics\", \"Priority Support\", \"API Access\", \"Custom Integrations\", \"Dedicated Support\"]");
        
        return subscriptionPlanRepository.persist(planBasico)
                .chain(savedPlan1 -> subscriptionPlanRepository.persist(planProfesional))
                .chain(savedPlan2 -> subscriptionPlanRepository.persist(planEmpresarial))
                .map(savedPlan3 -> ApiResponse.success("Plans created successfully", Map.of(
                        "plansCreated", 3,
                        "message", "Basic, Professional and Enterprise plans created"
                )));
    }

    private SubscriptionPlanEntity createPlan(String name, String description, double price, String billingCycle,
                                              int maxAdmins, int maxSellers, int tokensIncluded, String features) {
        SubscriptionPlanEntity plan = new SubscriptionPlanEntity();
        plan.name = name;
        plan.description = description;
        plan.pricePen = java.math.BigDecimal.valueOf(price);
        plan.billingCycle = billingCycle;
        plan.maxAdmins = maxAdmins;
        plan.maxSellers = maxSellers;
        plan.tokensIncluded = tokensIncluded;
        plan.features = features;
        plan.isActive = true;
        return plan;
    }
}
