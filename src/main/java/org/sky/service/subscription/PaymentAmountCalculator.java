package org.sky.service.subscription;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.repository.SubscriptionPlanRepository;

@ApplicationScoped
public class PaymentAmountCalculator {

    @Inject
    SubscriptionPlanRepository subscriptionPlanRepository;
    

    public Uni<Double> calculateAmount(Long planId) {
        if (planId != null) {
            return calculatePlanAmount(planId);
        }
        return Uni.createFrom().item(0.0);
    }

    private Uni<Double> calculatePlanAmount(Long planId) {
        return subscriptionPlanRepository.findById(planId)
                .map(plan -> plan.pricePen.doubleValue())
                .onFailure().recoverWithItem(0.0);
    }

}
