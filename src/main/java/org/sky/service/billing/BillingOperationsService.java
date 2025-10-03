package org.sky.service.billing;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.response.ApiResponse;
import org.sky.dto.request.billing.PaymentRequest;
import org.sky.service.SubscriptionService;
import org.sky.service.subscription.PaymentCodeService;
import org.sky.service.subscription.PaymentUploadService;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class BillingOperationsService {

    @Inject
    PaymentCodeService paymentCodeService;
    
    @Inject
    PaymentUploadService paymentUploadService;
    
    @Inject
    SubscriptionService subscriptionService;
    

    public Uni<ApiResponse<Object>> executeGenerateCode(Long adminId, PaymentRequest request, Boolean validate) {
        if (request.planId() == null) {
            return Uni.createFrom().item(ApiResponse.error("planId is required for subscription"));
        }
        
        return paymentCodeService.generatePaymentCode(adminId, request.planId())
                .map(paymentCode -> ApiResponse.success("Payment code generated successfully", paymentCode));
    }

    public Uni<ApiResponse<Object>> executeUpload(Long adminId, PaymentRequest request, Boolean validate) {
        if (request.paymentCode() == null || request.imageBase64() == null) {
            return Uni.createFrom().item(ApiResponse.error("paymentCode and imageBase64 are required to upload image"));
        }
        
        return paymentUploadService.uploadPaymentImage(adminId, request.paymentCode(), request.imageBase64())
                .map(uploadResponse -> ApiResponse.success("Image uploaded successfully", uploadResponse));
    }

    public Uni<ApiResponse<Object>> executeSubscribe(Long adminId, PaymentRequest request, Boolean validate) {
        if (request.planId() == null) {
            return Uni.createFrom().item(ApiResponse.error("planId is required for subscription"));
        }
        
        return subscriptionService.subscribeToPlan(adminId, request.planId())
                .map(subscriptionStatus -> ApiResponse.success("Subscription processed successfully", subscriptionStatus));
    }

    public Uni<ApiResponse<Object>> executeUpgrade(Long adminId, PaymentRequest request, Boolean validate) {
        if (request.planId() == null) {
            return Uni.createFrom().item(ApiResponse.error("planId is required for upgrade"));
        }
        
        return subscriptionService.upgradePlan(adminId, request.planId())
                .map(subscriptionStatus -> ApiResponse.success("Upgrade processed successfully", subscriptionStatus));
    }

    public Uni<ApiResponse<Object>> executeCancel(Long adminId, PaymentRequest request, Boolean validate) {
        return subscriptionService.cancelSubscription(adminId)
                .map(subscriptionStatus -> ApiResponse.success("Cancellation processed successfully", subscriptionStatus));
    }


    public Uni<ApiResponse<Object>> executeCheck(Long adminId, PaymentRequest request) {
        return subscriptionService.getSubscriptionStatus(adminId)
                .map(subscriptionStatus -> {
                    boolean hasActiveSubscription = subscriptionStatus.isActive();
                    
                    String message = String.format("Active subscription: %s", hasActiveSubscription);
                    
                    return ApiResponse.success("Verification completed", Map.of(
                            "hasActiveSubscription", hasActiveSubscription,
                            "subscriptionStatus", subscriptionStatus,
                            "message", message
                    ));
                });
    }

    public Uni<ApiResponse<Object>> executeSimulate(Long adminId, PaymentRequest request) {
        return subscriptionService.getSubscriptionStatus(adminId)
                .map(subscriptionStatus -> {
                    Map<String, Object> simulation = Map.of(
                            "currentPlan", subscriptionStatus.planName(),
                            "simulatedOperations", List.of(
                                    Map.of("operation", "payment_processing", "canExecute", subscriptionStatus.isActive()),
                                    Map.of("operation", "qr_generation", "canExecute", subscriptionStatus.isActive()),
                                    Map.of("operation", "analytics_report", "canExecute", subscriptionStatus.isActive())
                            ),
                            "recommendations", List.of(
                                    !subscriptionStatus.isActive() ? "Consider activating a subscription" : "Active subscription"
                            )
                    );
                    
                    return ApiResponse.success("Simulation completed", simulation);
                });
    }
}
