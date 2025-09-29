package org.sky.service.payment;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.billing.PaymentUploadResponse;
import org.sky.model.ManualPayment;
import org.sky.model.PaymentCode;
import org.sky.repository.ManualPaymentRepository;

@ApplicationScoped
public class PaymentUploadService {

    @Inject
    ManualPaymentRepository manualPaymentRepository;
    
    @Inject
    PaymentCodeService paymentCodeService;
    
    @Inject
    PaymentImageValidator imageValidator;

    @WithTransaction
    public Uni<PaymentUploadResponse> uploadPaymentImage(Long adminId, String paymentCode, String imageBase64) {
        return imageValidator.validateImage(imageBase64)
                .chain(valid -> paymentCodeService.findValidCode(paymentCode)
                        .chain(code -> validatePaymentCode(code)
                                .chain(validCode -> createManualPayment(adminId, validCode, imageBase64))));
    }

    private Uni<PaymentCode> validatePaymentCode(PaymentCode code) {
        if (code == null) {
            return Uni.createFrom().failure(new RuntimeException("Invalid or expired payment code"));
        }
        return Uni.createFrom().item(code);
    }

    private Uni<PaymentUploadResponse> createManualPayment(Long adminId, PaymentCode code, String imageBase64) {
        ManualPayment payment = new ManualPayment();
        payment.paymentCodeId = code.id;
        payment.adminId = adminId;
        payment.imageBase64 = imageBase64;
        payment.amountPen = code.amountPen;
        payment.yapeNumber = code.yapeNumber;
        payment.status = "pending";
        
        return manualPaymentRepository.persist(payment)
                .map(savedPayment -> new PaymentUploadResponse(
                    savedPayment.id,
                    "Image uploaded successfully. Waiting for administrator review.",
                    "pending"
                ));
    }
}
