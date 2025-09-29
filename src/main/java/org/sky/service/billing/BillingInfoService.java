package org.sky.service.billing;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.ApiResponse;
import org.sky.service.TokenService;
import org.sky.service.SubscriptionService;
import org.sky.service.subscription.PaymentHistoryService;

@ApplicationScoped
public class BillingInfoService {

    @Inject
    TokenService tokenService;
    
    @Inject
    SubscriptionService subscriptionService;
    
    @Inject
    PaymentHistoryService paymentHistoryService;

    public Uni<ApiResponse<Object>> getTokenStatus(Long adminId, String period, String include) {
        return tokenService.getTokenStatus(adminId)
                .map(tokenStatus -> ApiResponse.success("Token status retrieved successfully", tokenStatus));
    }

    public Uni<ApiResponse<Object>> getSubscriptionStatus(Long adminId, String period, String include) {
        return subscriptionService.getSubscriptionStatus(adminId)
                .map(subscriptionStatus -> ApiResponse.success("Subscription status retrieved successfully", subscriptionStatus));
    }

    public Uni<ApiResponse<Object>> getPaymentHistory(Long adminId, String period, String include) {
        return paymentHistoryService.getPaymentHistory(adminId, period)
                .map(paymentHistory -> ApiResponse.success("Payment history retrieved successfully", paymentHistory));
    }
}
