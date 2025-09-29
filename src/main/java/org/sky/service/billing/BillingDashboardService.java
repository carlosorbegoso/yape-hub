package org.sky.service.billing;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.ApiResponse;
import org.sky.dto.billing.BillingDashboardResponse;
import org.sky.dto.billing.BillingSummaryResponse;
import org.sky.dto.billing.MonthlyUsageResponse;
import org.sky.dto.billing.TokenStatusResponse;
import org.sky.dto.billing.SubscriptionStatusResponse;
import org.sky.service.TokenService;
import org.sky.service.SubscriptionService;

import java.util.List;

@ApplicationScoped
public class BillingDashboardService {

    @Inject
    TokenService tokenService;
    
    @Inject
    SubscriptionService subscriptionService;

    public Uni<ApiResponse<BillingDashboardResponse>> getDashboard(Long adminId, String period, String include) {
        return tokenService.getTokenStatus(adminId)
                .chain(tokenStatus -> subscriptionService.getSubscriptionStatus(adminId)
                        .map(subscriptionStatus -> buildDashboard(adminId, tokenStatus, subscriptionStatus)));
    }

    private ApiResponse<BillingDashboardResponse> buildDashboard(Long adminId, TokenStatusResponse tokenStatus, 
                                                               SubscriptionStatusResponse subscriptionStatus) {
        BillingDashboardResponse dashboard = new BillingDashboardResponse(
                adminId,
                tokenStatus,
                subscriptionStatus,
                List.of(), // Lista vacía de historial
                new MonthlyUsageResponse(
                        tokenStatus.tokensUsed().longValue(),
                        tokenStatus.tokensAvailable().longValue(),
                        0L, // Sin operaciones por ahora
                        "N/A" // Sin operación más usada
                ),
                new BillingSummaryResponse(
                        0.0, // Sin gastos por ahora
                        "PEN",
                        subscriptionStatus.endDate(),
                        subscriptionStatus.isActive(),
                        "manual"
                ),
                java.time.LocalDateTime.now()
        );
        
        return ApiResponse.success("Dashboard obtenido exitosamente", dashboard);
    }
}
