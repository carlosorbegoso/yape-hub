package org.sky.service.subscription;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.response.billing.PaymentUploadResponse;
import org.sky.model.ManualPaymentEntity;
import org.sky.model.PaymentCodeEntity;
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
        return Uni.combine().all().unis(
                imageValidator.validateImage(imageBase64),
                paymentCodeService.findValidCode(paymentCode)
        ).asTuple()
        .chain(tuple -> {
            PaymentCodeEntity code = tuple.getItem2();
            if (code == null) {
                return Uni.createFrom().failure(new RuntimeException("Invalid or expired payment code"));
            }
            return createManualPayment(adminId, code, imageBase64);
        });
    }


    private Uni<PaymentUploadResponse> createManualPayment(Long adminId, PaymentCodeEntity code, String imageBase64) {
        ManualPaymentEntity payment = new ManualPaymentEntity();
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
