package org.sky.service.subscription;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.billing.PaymentCodeResponse;
import org.sky.dto.billing.PaymentStatusResponse;
import org.sky.model.PaymentCode;
import org.sky.repository.PaymentCodeRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class PaymentCodeService {

    @Inject
    PaymentCodeRepository paymentCodeRepository;
    
    @Inject
    PaymentAmountCalculator amountCalculator;

    private static final String YAPE_NUMBER = "977737772";

    @WithTransaction
    public Uni<PaymentCodeResponse> generatePaymentCode(Long adminId, Long planId, String tokensPackage) {
        return Uni.combine().all().unis(
                generateUniqueCode(),
                amountCalculator.calculateAmount(planId, tokensPackage)
        ).asTuple()
        .chain(tuple -> createPaymentCode(adminId, planId, tokensPackage, tuple.getItem1(), tuple.getItem2()));
    }

    @WithTransaction
    public Uni<PaymentStatusResponse> getPaymentStatus(String paymentCode) {
        return paymentCodeRepository.findByCode(paymentCode)
                .chain(code -> code != null ? 
                    processValidCode(paymentCode, code) : 
                    createNotFoundStatus(paymentCode));
    }

    @WithTransaction
    public Uni<PaymentCode> markAsPaid(Long paymentCodeId) {
        return paymentCodeRepository.findById(paymentCodeId)
                .chain(code -> {
                    code.markAsPaid();
                    return paymentCodeRepository.persist(code);
                });
    }

    public Uni<PaymentCode> findValidCode(String paymentCode) {
        return paymentCodeRepository.findValidCode(paymentCode);
    }

    private Uni<PaymentCodeResponse> createPaymentCode(Long adminId, Long planId, String tokensPackage, 
                                                      String paymentCode, Double amount) {
        PaymentCode paymentCodeEntity = new PaymentCode();
        paymentCodeEntity.code = paymentCode;
        paymentCodeEntity.adminId = adminId;
        paymentCodeEntity.planId = planId;
        paymentCodeEntity.tokensPackage = tokensPackage;
        paymentCodeEntity.amountPen = BigDecimal.valueOf(amount);
        paymentCodeEntity.yapeNumber = YAPE_NUMBER;
        paymentCodeEntity.status = "pending";
        paymentCodeEntity.expiresAt = LocalDateTime.now().plusHours(24);
        
        return paymentCodeRepository.persist(paymentCodeEntity)
                .map(savedCode -> new PaymentCodeResponse(
                    paymentCode,
                    YAPE_NUMBER,
                    amount,
                    "PEN",
                    savedCode.expiresAt,
                    "Make the payment and upload the screenshot"
                ));
    }

    private Uni<String> generateUniqueCode() {
        return Uni.createFrom().item(() -> 
            "PAY_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );
    }

    private Uni<PaymentStatusResponse> processValidCode(String paymentCode, PaymentCode code) {
        boolean isExpired = code.isExpired();
        String status = isExpired ? "expired" : code.status;
        
        return getStatusMessage(status, isExpired)
                .map(message -> new PaymentStatusResponse(
                    paymentCode,
                    status,
                    code.amountPen.doubleValue(),
                    "PEN",
                    code.expiresAt,
                    isExpired,
                    message
                ));
    }

    private Uni<PaymentStatusResponse> createNotFoundStatus(String paymentCode) {
        return Uni.createFrom().item(() -> new PaymentStatusResponse(
            paymentCode,
            "not_found",
            0.0,
            "PEN",
            null,
            false,
            "Payment code not found"
        ));
    }

    private Uni<String> getStatusMessage(String status, boolean isExpired) {
        return Uni.createFrom().item(() -> switch (status) {
            case "pending" -> isExpired ? "Payment code expired" : "Payment pending review";
            case "paid" -> "Payment approved and processed";
            case "expired" -> "Payment code expired";
            default -> "Unknown status";
        });
    }
}
