package org.sky.service;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.billing.SubscriptionPlanResponse;
import org.sky.dto.billing.SubscriptionStatusResponse;
import org.sky.model.AdminSubscription;
import org.sky.model.SubscriptionPlan;
import org.sky.repository.AdminSubscriptionRepository;
import org.sky.repository.SubscriptionPlanRepository;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class SubscriptionService {

    @Inject
    SubscriptionPlanRepository subscriptionPlanRepository;

    @Inject
    AdminSubscriptionRepository adminSubscriptionRepository;

    @Inject
    TokenService tokenService;

    @WithTransaction
    public Uni<List<SubscriptionPlanResponse>> getAvailablePlans() {
        Log.info("📋 SubscriptionService.getAvailablePlans()");
        
        return subscriptionPlanRepository.findAll()
                .list()
                .map(plans -> plans.stream()
                        .map(plan -> new SubscriptionPlanResponse(
                                plan.id,
                                plan.name,
                                plan.description,
                                plan.pricePen.doubleValue(),
                                plan.billingCycle,
                                plan.maxAdmins,
                                plan.maxSellers,
                                plan.tokensIncluded,
                                plan.features,
                                plan.isActive,
                                plan.createdAt
                        ))
                        .toList());
    }

    @WithTransaction
    public Uni<SubscriptionStatusResponse> getSubscriptionStatus(Long adminId) {
        Log.info("📋 SubscriptionService.getSubscriptionStatus() - AdminId: " + adminId);
        
        return adminSubscriptionRepository.findActiveByAdminId(adminId)
                .chain(subscription -> {
                    if (subscription == null) {
                        return Uni.createFrom().item(new SubscriptionStatusResponse(
                                null,
                                "free",
                                "Plan Gratuito",
                                "Plan básico con funcionalidades limitadas",
                                0.0,
                                "PEN",
                                "monthly",
                                1,
                                100,
                                LocalDateTime.now(),
                                null,
                                false,
                                "Sin suscripción activa"
                        ));
                    }
                    
                    return subscriptionPlanRepository.findById(subscription.planId)
                            .map(plan -> new SubscriptionStatusResponse(
                                    subscription.id,
                                    subscription.status,
                                    plan.name,
                                    plan.description,
                                    plan.pricePen.doubleValue(),
                                    "PEN",
                                    plan.billingCycle,
                                    plan.maxSellers,
                                    plan.tokensIncluded,
                                    subscription.startDate,
                                    subscription.endDate,
                                    subscription.isActive(),
                                    "Suscripción activa"
                            ));
                });
    }

    @WithTransaction
    public Uni<SubscriptionStatusResponse> subscribeToPlan(Long adminId, Long planId) {
        Log.info("📋 SubscriptionService.subscribeToPlan() - AdminId: " + adminId + ", PlanId: " + planId);
        
        return subscriptionPlanRepository.findById(planId)
                .chain(plan -> {
                    if (plan == null) {
                        return Uni.createFrom().failure(new RuntimeException("Plan no encontrado"));
                    }
                    
                    // Verificar si ya tiene una suscripción activa
                    return adminSubscriptionRepository.findActiveByAdminId(adminId)
                            .chain(existingSubscription -> {
                                if (existingSubscription != null) {
                                    return Uni.createFrom().failure(new RuntimeException("Ya tienes una suscripción activa"));
                                }
                                
                                // Crear nueva suscripción
                                AdminSubscription subscription = new AdminSubscription();
                                subscription.adminId = adminId;
                                subscription.planId = planId;
                                subscription.status = "active";
                                subscription.startDate = LocalDateTime.now();
                                subscription.endDate = LocalDateTime.now().plusMonths(1); // Por defecto mensual
                                
                                return adminSubscriptionRepository.persist(subscription)
                                        .chain(savedSubscription -> {
                                            // Agregar tokens incluidos en el plan
                                            return tokenService.addTokens(adminId, plan.tokensIncluded)
                                                    .map(tokens -> new SubscriptionStatusResponse(
                                                            savedSubscription.id,
                                                            savedSubscription.status,
                                                            plan.name,
                                                            plan.description,
                                                            plan.pricePen.doubleValue(),
                                                            "PEN",
                                                            plan.billingCycle,
                                                            plan.maxSellers,
                                                            plan.tokensIncluded,
                                                            savedSubscription.startDate,
                                                            savedSubscription.endDate,
                                                            true,
                                                            "Suscripción activada exitosamente"
                                                    ));
                                        });
                            });
                });
    }

    @WithTransaction
    public Uni<SubscriptionStatusResponse> upgradePlan(Long adminId, Long newPlanId) {
        Log.info("⬆️ SubscriptionService.upgradePlan() - AdminId: " + adminId + ", NewPlanId: " + newPlanId);
        
        return subscriptionPlanRepository.findById(newPlanId)
                .chain(newPlan -> {
                    if (newPlan == null) {
                        return Uni.createFrom().failure(new RuntimeException("Plan no encontrado"));
                    }
                    
                    return adminSubscriptionRepository.findActiveByAdminId(adminId)
                            .chain(existingSubscription -> {
                                if (existingSubscription == null) {
                                    return Uni.createFrom().failure(new RuntimeException("No tienes una suscripción activa para actualizar"));
                                }
                                
                                // Actualizar suscripción
                                existingSubscription.planId = newPlanId;
                                existingSubscription.endDate = LocalDateTime.now().plusMonths(1);
                                
                                return adminSubscriptionRepository.persist(existingSubscription)
                                        .map(updatedSubscription -> new SubscriptionStatusResponse(
                                                updatedSubscription.id,
                                                updatedSubscription.status,
                                                newPlan.name,
                                                newPlan.description,
                                                newPlan.pricePen.doubleValue(),
                                                "PEN",
                                                newPlan.billingCycle,
                                                newPlan.maxSellers,
                                                newPlan.tokensIncluded,
                                                updatedSubscription.startDate,
                                                updatedSubscription.endDate,
                                                true,
                                                "Plan actualizado exitosamente"
                                        ));
                            });
                });
    }

    @WithTransaction
    public Uni<SubscriptionStatusResponse> cancelSubscription(Long adminId) {
        Log.info("❌ SubscriptionService.cancelSubscription() - AdminId: " + adminId);
        
        return adminSubscriptionRepository.findActiveByAdminId(adminId)
                .chain(subscription -> {
                    if (subscription == null) {
                        return Uni.createFrom().failure(new RuntimeException("No tienes una suscripción activa para cancelar"));
                    }
                    
                    // Cancelar suscripción
                    subscription.status = "cancelled";
                    subscription.endDate = LocalDateTime.now();
                    
                    return adminSubscriptionRepository.persist(subscription)
                            .map(cancelledSubscription -> new SubscriptionStatusResponse(
                                    cancelledSubscription.id,
                                    cancelledSubscription.status,
                                    "Plan Cancelado",
                                    "Tu suscripción ha sido cancelada",
                                    0.0,
                                    "PEN",
                                    "none",
                                    1,
                                    100,
                                    cancelledSubscription.startDate,
                                    cancelledSubscription.endDate,
                                    false,
                                    "Suscripción cancelada exitosamente"
                            ));
                });
    }

    @WithTransaction
    public Uni<Boolean> checkSubscriptionLimits(Long adminId, int sellersNeeded) {
        Log.info("🔍 SubscriptionService.checkSubscriptionLimits() - AdminId: " + adminId + ", SellersNeeded: " + sellersNeeded);
        
        return adminSubscriptionRepository.findActiveByAdminId(adminId)
                .chain(subscription -> {
                    if (subscription == null) {
                        // Plan gratuito: máximo 2 vendedores
                        return Uni.createFrom().item(sellersNeeded <= 2);
                    }
                    
                    return subscriptionPlanRepository.findById(subscription.planId)
                            .map(plan -> sellersNeeded <= plan.maxSellers);
                });
    }
}
