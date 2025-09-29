package org.sky.service.billing;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.ApiResponse;
import org.sky.dto.billing.PaymentRequest;
import org.sky.dto.billing.SubscriptionStatusResponse;
import org.sky.dto.billing.TokenStatusResponse;
import org.sky.service.SubscriptionService;
import org.sky.service.TokenService;
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
    
    @Inject
    TokenService tokenService;

    public Uni<ApiResponse<Object>> executeGenerateCode(Long adminId, PaymentRequest request, Boolean validate) {
        if (!request.isValid()) {
            return Uni.createFrom().item(ApiResponse.error("You must specify planId for subscription or tokensPackage for token purchase"));
        }
        
        return paymentCodeService.generatePaymentCode(adminId, request.planId(), request.tokensPackage())
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

    public Uni<ApiResponse<Object>> executePurchase(Long adminId, PaymentRequest request, Boolean validate) {
        if (request.tokensPackage() == null) {
            return Uni.createFrom().item(ApiResponse.error("tokensPackage is required for purchase"));
        }
        
        return paymentCodeService.generatePaymentCode(adminId, null, request.tokensPackage())
                .map(paymentCode -> ApiResponse.success("Payment code generated for token purchase", paymentCode));
    }

    public Uni<ApiResponse<Object>> executeCheck(Long adminId, PaymentRequest request) {
        return Uni.combine().all()
                .unis(
                    tokenService.getTokenStatus(adminId),
                    subscriptionService.getSubscriptionStatus(adminId)
                )
                .asTuple()
                .map(tuple -> {
                    TokenStatusResponse tokenStatus = tuple.getItem1();
                    SubscriptionStatusResponse subscriptionStatus = tuple.getItem2();
                    
                    boolean hasTokens = tokenStatus.tokensAvailable() > 0;
                    boolean hasActiveSubscription = subscriptionStatus.isActive();
                    
                    String message = String.format("Tokens available: %d, Active subscription: %s", 
                            tokenStatus.tokensAvailable(), hasActiveSubscription);
                    
                    return ApiResponse.success("Verification completed", Map.of(
                            "hasTokens", hasTokens,
                            "hasActiveSubscription", hasActiveSubscription,
                            "tokenStatus", tokenStatus,
                            "subscriptionStatus", subscriptionStatus,
                            "message", message
                    ));
                });
    }

    public Uni<ApiResponse<Object>> executeSimulate(Long adminId, PaymentRequest request) {
        return Uni.combine().all()
                .unis(
                    tokenService.getTokenStatus(adminId),
                    subscriptionService.getSubscriptionStatus(adminId)
                )
                .asTuple()
                .map(tuple -> {
                    TokenStatusResponse tokenStatus = tuple.getItem1();
                    SubscriptionStatusResponse subscriptionStatus = tuple.getItem2();
                    
                    Map<String, Object> simulation = Map.of(
                            "currentTokens", tokenStatus.tokensAvailable(),
                            "currentPlan", subscriptionStatus.planName(),
                            "simulatedOperations", List.of(
                                    Map.of("operation", "payment_processing", "tokensNeeded", 1, "canExecute", tokenStatus.tokensAvailable() >= 1),
                                    Map.of("operation", "qr_generation", "tokensNeeded", 1, "canExecute", tokenStatus.tokensAvailable() >= 1),
                                    Map.of("operation", "analytics_report", "tokensNeeded", 2, "canExecute", tokenStatus.tokensAvailable() >= 2)
                            ),
                            "recommendations", List.of(
                                    tokenStatus.tokensAvailable() < 10 ? "Consider purchasing more tokens" : "You have enough tokens",
                                    !subscriptionStatus.isActive() ? "Consider activating a subscription" : "Active subscription"
                            )
                    );
                    
                    return ApiResponse.success("Simulation completed", simulation);
                });
    }
}
