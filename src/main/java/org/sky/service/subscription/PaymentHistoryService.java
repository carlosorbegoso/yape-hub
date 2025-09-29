package org.sky.service.subscription;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.billing.PaymentHistoryResponse;
import org.sky.model.PaymentHistory;
import org.sky.repository.PaymentHistoryRepository;

import java.util.List;

@ApplicationScoped
public class PaymentHistoryService {

    @Inject
    PaymentHistoryRepository paymentHistoryRepository;

    @WithTransaction
    public Uni<List<PaymentHistoryResponse>> getPaymentHistory(Long adminId, String period) {
        return paymentHistoryRepository.findByAdminId(adminId)
                .onFailure().recoverWithItem(throwable -> List.<PaymentHistory>of())
                .map(this::convertToResponseList);
    }

    private List<PaymentHistoryResponse> convertToResponseList(List<PaymentHistory> payments) {
        if (payments == null || payments.isEmpty()) {
            return List.of();
        }
        
        return payments.stream()
                .map(this::convertToResponse)
                .toList();
    }

    private PaymentHistoryResponse convertToResponse(PaymentHistory payment) {
        return new PaymentHistoryResponse(
            payment.id,
            payment.adminId,
            payment.paymentType,
            payment.amountPen.doubleValue(),
            "PEN",
            payment.status,
            payment.paymentMethod,
            payment.notes,
            payment.createdAt,
            payment.createdAt, // Use createdAt as processedAt for now
            payment.transactionId
        );
    }
}
