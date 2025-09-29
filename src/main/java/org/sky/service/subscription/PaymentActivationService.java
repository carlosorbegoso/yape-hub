package org.sky.service.subscription;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.model.PaymentCode;
import org.sky.service.TokenService;
import org.sky.service.SubscriptionService;

@ApplicationScoped
public class PaymentActivationService {

    @Inject
    TokenService tokenService;
    
    @Inject
    SubscriptionService subscriptionService;

    public Uni<String> activatePlanOrTokens(Long adminId, PaymentCode code) {
        return Uni.createFrom().item(code)
                .chain(c -> {
                    if (c.planId != null) {
                        return activateSubscription(adminId, c.planId);
                    } else if (c.tokensPackage != null) {
                        return activateTokens(adminId, c.tokensPackage);
                    }
                    return Uni.createFrom().item("Activation completed");
                });
    }

    private Uni<String> activateSubscription(Long adminId, Long planId) {
        return subscriptionService.subscribeToPlan(adminId, planId)
                .map(subscriptionStatus -> "Subscription activated: " + subscriptionStatus.planName() + 
                     " (ID: " + subscriptionStatus.subscriptionId() + ")");
    }

    private Uni<String> activateTokens(Long adminId, String tokensPackage) {
        int tokensToAdd = Integer.parseInt(tokensPackage);
        return tokenService.addTokens(adminId, tokensToAdd)
                .map(result -> "Tokens added: " + tokensToAdd);
    }
}
