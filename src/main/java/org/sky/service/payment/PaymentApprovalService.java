package org.sky.service.payment;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.billing.PaymentUploadResponse;
import org.sky.model.ManualPayment;
import org.sky.model.PaymentCode;
import org.sky.repository.ManualPaymentRepository;
import org.sky.service.TokenService;

@ApplicationScoped
public class PaymentApprovalService {

    @Inject
    ManualPaymentRepository manualPaymentRepository;
    
    @Inject
    PaymentCodeService paymentCodeService;
    
    @Inject
    PaymentActivationService activationService;

    @WithTransaction
    public Uni<PaymentUploadResponse> approvePayment(Long adminId, Long paymentId, String reviewNotes) {
        return manualPaymentRepository.findById(paymentId)
                .chain(payment -> validatePendingPayment(payment)
                        .chain(validPayment -> approvePaymentAndActivate(adminId, validPayment, reviewNotes)));
    }

    @WithTransaction
    public Uni<PaymentUploadResponse> rejectPayment(Long adminId, Long paymentId, String reviewNotes) {
        return manualPaymentRepository.findById(paymentId)
                .chain(payment -> validatePendingPayment(payment)
                        .chain(validPayment -> rejectPayment(adminId, validPayment, reviewNotes)));
    }

    private Uni<ManualPayment> validatePendingPayment(ManualPayment payment) {
        if (payment == null || !payment.isPending()) {
            return Uni.createFrom().failure(new RuntimeException("Payment not found or already processed"));
        }
        return Uni.createFrom().item(payment);
    }

    private Uni<PaymentUploadResponse> approvePaymentAndActivate(Long adminId, ManualPayment payment, String reviewNotes) {
        payment.approve(adminId, reviewNotes);
        
        return manualPaymentRepository.persist(payment)
                .chain(savedPayment -> paymentCodeService.markAsPaid(payment.paymentCodeId)
                        .chain(updatedCode -> activationService.activatePlanOrTokens(payment.adminId, updatedCode)
                                .map(activation -> new PaymentUploadResponse(
                                    payment.id,
                                    "Payment approved successfully",
                                    "approved"
                                ))));
    }

    private Uni<PaymentUploadResponse> rejectPayment(Long adminId, ManualPayment payment, String reviewNotes) {
        payment.reject(adminId, reviewNotes);
        
        return manualPaymentRepository.persist(payment)
                .map(savedPayment -> new PaymentUploadResponse(
                    payment.id,
                    "Payment rejected: " + reviewNotes,
                    "rejected"
                ));
    }
}
