package org.sky.service;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.response.billing.SubscriptionStatusResponse;
import org.sky.model.AdminSubscriptionEntity;
import org.sky.repository.AdminSubscriptionRepository;
import org.sky.repository.SubscriptionPlanRepository;

import java.time.LocalDateTime;

@ApplicationScoped
public class SubscriptionService {

    @Inject
    SubscriptionPlanRepository subscriptionPlanRepository;

    @Inject
    AdminSubscriptionRepository adminSubscriptionRepository;


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
                                AdminSubscriptionEntity subscription = new AdminSubscriptionEntity();
                                subscription.adminId = adminId;
                                subscription.planId = planId;
                                subscription.status = "active";
                                subscription.startDate = LocalDateTime.now();
                                // Calcular fecha de fin según el ciclo de facturación del plan
                                subscription.endDate = calculateEndDate(plan.billingCycle);
                                
                                return adminSubscriptionRepository.persist(subscription)
                                        .map(savedSubscription -> new SubscriptionStatusResponse(
                                                savedSubscription.id,
                                                savedSubscription.status,
                                                plan.name,
                                                plan.description,
                                                plan.pricePen.doubleValue(),
                                                "PEN",
                                                plan.billingCycle,
                                                plan.maxSellers,
                                                savedSubscription.startDate,
                                                savedSubscription.endDate,
                                                true,
                                                "Suscripción activada exitosamente"
                                        ));
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
                                existingSubscription.endDate = calculateEndDate(newPlan.billingCycle);
                                
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
                                    cancelledSubscription.startDate,
                                    cancelledSubscription.endDate,
                                    false,
                                    "Suscripción cancelada exitosamente"
                            ));
                });
    }

    @WithTransaction
    public Uni<SubscriptionStatusResponse> subscribeToFreePlan(Long adminId) {
        Log.info("📋 SubscriptionService.subscribeToFreePlan() - AdminId: " + adminId);
        
        return subscriptionPlanRepository.findByName("Plan Gratuito")
                .chain(freePlan -> {
                    if (freePlan == null) {
                        return Uni.createFrom().failure(new RuntimeException("Plan Gratuito no encontrado en la base de datos"));
                    }
                    
                    // Verificar si ya tiene una suscripción activa
                    return adminSubscriptionRepository.findActiveByAdminId(adminId)
                            .chain(existingSubscription -> {
                                if (existingSubscription != null) {
                                    // Ya tiene suscripción, retornar la existente
                                    return subscriptionPlanRepository.findById(existingSubscription.planId)
                                            .map(plan -> new SubscriptionStatusResponse(
                                                    existingSubscription.id,
                                                    existingSubscription.status,
                                                    plan.name,
                                                    plan.description,
                                                    plan.pricePen.doubleValue(),
                                                    "PEN",
                                                    plan.billingCycle,
                                                    plan.maxSellers,
                                                    existingSubscription.startDate,
                                                    existingSubscription.endDate,
                                                    existingSubscription.isActive(),
                                                    "Suscripción existente"
                                            ));
                                }
                                
                                // Crear nueva suscripción gratuita
                                AdminSubscriptionEntity subscription = new AdminSubscriptionEntity();
                                subscription.adminId = adminId;
                                subscription.planId = freePlan.id;
                                subscription.status = "active";
                                subscription.startDate = LocalDateTime.now();
                                subscription.endDate = null; // Plan gratuito sin expiración
                                
                                return adminSubscriptionRepository.persist(subscription)
                                        .map(savedSubscription -> new SubscriptionStatusResponse(
                                                savedSubscription.id,
                                                savedSubscription.status,
                                                freePlan.name,
                                                freePlan.description,
                                                freePlan.pricePen.doubleValue(),
                                                "PEN",
                                                freePlan.billingCycle,
                                                freePlan.maxSellers,
                                                savedSubscription.startDate,
                                                savedSubscription.endDate,
                                                true,
                                                "Suscripción gratuita activada exitosamente"
                                        ));
                            });
                });
    }

    @WithTransaction
    public Uni<Boolean> checkSubscriptionLimits(Long adminId, int sellersNeeded) {
        Log.info("🔍 SubscriptionService.checkSubscriptionLimits() - AdminId: " + adminId + ", SellersNeeded: " + sellersNeeded);
        
        return adminSubscriptionRepository.findActiveByAdminId(adminId)
                .chain(subscription -> {
                    if (subscription == null) {
                        // Buscar plan gratuito en la base de datos
                        return subscriptionPlanRepository.findByName("Plan Gratuito")
                                .map(freePlan -> {
                                    if (freePlan == null) {
                                        // Fallback: si no existe el plan gratuito, usar límite de 1
                                        Log.warn("⚠️ Plan Gratuito no encontrado, usando límite por defecto de 1 vendedor");
                                        return sellersNeeded <= 1;
                                    }
                                    Log.info("📋 Usando Plan Gratuito - Límite: " + freePlan.maxSellers + ", Necesarios: " + sellersNeeded);
                                    return sellersNeeded <= freePlan.maxSellers;
                                });
                    }
                    
                    return subscriptionPlanRepository.findById(subscription.planId)
                            .map(plan -> {
                                Log.info("📋 Usando Plan: " + plan.name + " - Límite: " + plan.maxSellers + ", Necesarios: " + sellersNeeded);
                                return sellersNeeded <= plan.maxSellers;
                            });
                });
    }

    /**
     * Obtiene información detallada sobre los límites de vendedores para un admin
     */
    @WithTransaction
    public Uni<SellerLimitsInfo> getSellerLimitsInfo(Long adminId) {
        Log.info("📊 SubscriptionService.getSellerLimitsInfo() - AdminId: " + adminId);
        
        return adminSubscriptionRepository.findActiveByAdminId(adminId)
                .chain(subscription -> {
                    if (subscription == null) {
                        // Buscar plan gratuito
                        return subscriptionPlanRepository.findByName("Plan Gratuito")
                                .map(freePlan -> {
                                    if (freePlan == null) {
                                        return new SellerLimitsInfo(1, 0, "Plan Gratuito (Fallback)", true);
                                    }
                                    return new SellerLimitsInfo(freePlan.maxSellers, 0, freePlan.name, true);
                                });
                    }
                    
                    return subscriptionPlanRepository.findById(subscription.planId)
                            .map(plan -> new SellerLimitsInfo(plan.maxSellers, 0, plan.name, subscription.isActive()));
                });
    }

    /**
     * Clase para información de límites de vendedores
     */
    public record SellerLimitsInfo(
        int maxSellers,
        int currentSellers,
        String planName,
        boolean isActive
    ) {}

    /**
     * Calcula la fecha de fin de la suscripción según el ciclo de facturación
     */
    private LocalDateTime calculateEndDate(String billingCycle) {
        LocalDateTime now = LocalDateTime.now();
        return switch (billingCycle.toLowerCase()) {
            case "monthly" -> now.plusMonths(1);
            case "quarterly" -> now.plusMonths(3);
            case "semiannually" -> now.plusMonths(6);
            case "yearly" -> now.plusYears(1);
            case "biennially" -> now.plusYears(2);
            default -> now.plusMonths(1); // Por defecto mensual
        };
    }

    /**
     * Renueva una suscripción activa
     */
    @WithTransaction
    public Uni<SubscriptionStatusResponse> renewSubscription(Long adminId) {
        Log.info("🔄 SubscriptionService.renewSubscription() - AdminId: " + adminId);
        
        return adminSubscriptionRepository.findActiveByAdminId(adminId)
                .chain(subscription -> {
                    if (subscription == null) {
                        return Uni.createFrom().failure(new RuntimeException("No tienes una suscripción activa para renovar"));
                    }
                    
                    return subscriptionPlanRepository.findById(subscription.planId)
                            .chain(plan -> {
                                // Extender la fecha de fin según el ciclo de facturación
                                subscription.endDate = calculateEndDate(plan.billingCycle);
                                
                                return adminSubscriptionRepository.persist(subscription)
                                        .map(renewedSubscription -> new SubscriptionStatusResponse(
                                                renewedSubscription.id,
                                                renewedSubscription.status,
                                                plan.name,
                                                plan.description,
                                                plan.pricePen.doubleValue(),
                                                "PEN",
                                                plan.billingCycle,
                                                plan.maxSellers,
                                                renewedSubscription.startDate,
                                                renewedSubscription.endDate,
                                                true,
                                                "Suscripción renovada exitosamente"
                                        ));
                            });
                });
    }
}
