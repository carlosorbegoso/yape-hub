package org.sky.service.billing;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.response.ApiResponse;
import org.sky.dto.response.billing.BillingDashboardResponse;
import org.sky.dto.response.billing.BillingSummaryResponse;
import org.sky.dto.response.billing.SubscriptionStatusResponse;
import org.sky.service.SubscriptionService;

import java.util.List;

@ApplicationScoped
public class BillingDashboardService {

    
    @Inject
    SubscriptionService subscriptionService;

    public Uni<ApiResponse<BillingDashboardResponse>> getDashboard(Long adminId, String period, String include) {
        return subscriptionService.getSubscriptionStatus(adminId)
                .map(subscriptionStatus -> buildDashboard(adminId, subscriptionStatus));
    }

    private ApiResponse<BillingDashboardResponse> buildDashboard(Long adminId, SubscriptionStatusResponse subscriptionStatus) {
        BillingDashboardResponse dashboard = new BillingDashboardResponse(
                adminId,
                null, // Sin token status
                subscriptionStatus,
                List.of(), // Lista vacía de historial
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
