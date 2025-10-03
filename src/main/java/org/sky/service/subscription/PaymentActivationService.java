package org.sky.service.subscription;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.model.PaymentCodeEntity;
import org.sky.service.SubscriptionService;

@ApplicationScoped
public class PaymentActivationService {

    
    @Inject
    SubscriptionService subscriptionService;

    public Uni<String> activatePlan(Long adminId, PaymentCodeEntity code) {
        return Uni.createFrom().item(code)
                .chain(c -> {
                    if (c.planId != null) {
                        return activateSubscription(adminId, c.planId);
                    }
                    return Uni.createFrom().item("Activation completed");
                });
    }

    private Uni<String> activateSubscription(Long adminId, Long planId) {
        return subscriptionService.subscribeToPlan(adminId, planId)
                .map(subscriptionStatus -> "Subscription activated: " + subscriptionStatus.planName() + 
                     " (ID: " + subscriptionStatus.subscriptionId() + ")");
    }

}
