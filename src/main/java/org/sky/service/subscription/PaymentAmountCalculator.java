package org.sky.service.subscription;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.repository.SubscriptionPlanRepository;
import org.sky.repository.TokenPackageRepository;

@ApplicationScoped
public class PaymentAmountCalculator {

    @Inject
    SubscriptionPlanRepository subscriptionPlanRepository;
    
    @Inject
    TokenPackageRepository tokenPackageRepository;

    public Uni<Double> calculateAmount(Long planId, String tokensPackage) {
        return Uni.createFrom().item(() -> new AmountRequest(planId, tokensPackage))
                .chain(request -> {
                    if (request.planId() != null) {
                        return calculatePlanAmount(request.planId());
                    } else if (request.tokensPackage() != null) {
                        return calculateTokensAmount(request.tokensPackage());
                    }
                    return Uni.createFrom().item(0.0);
                });
    }

    private Uni<Double> calculatePlanAmount(Long planId) {
        return subscriptionPlanRepository.findById(planId)
                .map(plan -> plan.pricePen.doubleValue())
                .onFailure().recoverWithItem(0.0);
    }

    private Uni<Double> calculateTokensAmount(String tokensPackage) {
        return tokenPackageRepository.findByPackageId(tokensPackage)
                .map(packageEntity -> {
                    if (packageEntity == null) {
                        return 0.0;
                    }
                    return packageEntity.getDiscountedPrice().doubleValue();
                })
                .onFailure().recoverWithItem(0.0);
    }

    private record AmountRequest(Long planId, String tokensPackage) {}
}
