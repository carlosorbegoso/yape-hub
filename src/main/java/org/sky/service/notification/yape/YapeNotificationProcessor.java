package org.sky.service.notification.yape;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import org.sky.dto.response.ApiResponse;
import org.sky.dto.request.notification.YapeNotificationRequest;
import org.sky.dto.response.notification.YapeNotificationResponse;
import org.sky.dto.request.payment.PaymentNotificationRequest;
import org.sky.dto.response.payment.PaymentNotificationResponse;
import org.sky.model.YapeNotificationAuditEntity;
import org.sky.repository.YapeNotificationAuditRepository;
import org.sky.service.DeviceFingerprintService;
import org.sky.service.hubnotifications.PaymentNotificationService;
import org.sky.service.YapeDecryptionService;
import org.sky.exception.ValidationException;

import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class YapeNotificationProcessor {

    @Inject
    YapeNotificationAuditRepository yapeNotificationAuditRepository;
    
    @Inject
    YapeDecryptionService yapeDecryptionService;
    
    @Inject
    DeviceFingerprintService deviceFingerprintService;
    
    @Inject
    PaymentNotificationService paymentNotificationService;

    @WithTransaction
    public Uni<ApiResponse<YapeNotificationResponse>> processYapeNotification(YapeNotificationRequest request) {
        return createAuditRecord(request)
                .chain(auditRecord -> validateAndProcess(request, auditRecord));
    }

    private Uni<YapeNotificationAuditEntity> createAuditRecord(YapeNotificationRequest request) {
        YapeNotificationAuditEntity auditRecord = new YapeNotificationAuditEntity();
        auditRecord.adminId = request.adminId();
        auditRecord.encryptedNotification = request.encryptedNotification();
        auditRecord.deviceFingerprint = request.deviceFingerprint();
        auditRecord.timestamp = request.timestamp();
        auditRecord.deduplicationHash = request.deduplicationHash();
        auditRecord.decryptionStatus = "PENDING";

        return yapeNotificationAuditRepository.persist(auditRecord);
    }

    private Uni<ApiResponse<YapeNotificationResponse>> validateAndProcess(YapeNotificationRequest request, 
                                                                         YapeNotificationAuditEntity auditRecord) {
        return validateTimestamp(request)
                .chain(v -> validateDeviceFingerprint(request))
                .chain(v -> decryptNotification(request))
                .chain(decryptedResponse -> processPaymentAndUpdateAudit(request, decryptedResponse, auditRecord));
    }

    private Uni<Void> validateTimestamp(YapeNotificationRequest request) {
        return Uni.createFrom().completionStage(() -> {
            long currentTime = System.currentTimeMillis();
            long timeDiff = Math.abs(currentTime - request.timestamp());
            long maxTimeDiff = 5 * 60 * 1000; // 5 minutes

            if (timeDiff > maxTimeDiff) {
                return CompletableFuture.failedFuture(
                    ValidationException.invalidField("timestamp", request.timestamp().toString(),
                        "Timestamp too old. Difference: " + timeDiff + "ms")
                );
            }
            return CompletableFuture.completedFuture(null);
        });
    }

    private Uni<Void> validateDeviceFingerprint(YapeNotificationRequest request) {
        return Uni.createFrom().completionStage(() -> {
            try {
                deviceFingerprintService.validateDeviceFingerprint(request.deviceFingerprint());
                return CompletableFuture.completedFuture(null);
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
        });
    }

    private Uni<YapeNotificationResponse> decryptNotification(YapeNotificationRequest request) {
        return Uni.createFrom().completionStage(() -> {
            try {
                YapeNotificationResponse decryptedResponse =
                    yapeDecryptionService.decryptYapeNotification(
                        request.encryptedNotification(),
                        request.deviceFingerprint()
                    );
                return CompletableFuture.completedFuture(decryptedResponse);
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
        });
    }

    private Uni<ApiResponse<YapeNotificationResponse>> processPaymentAndUpdateAudit(
            YapeNotificationRequest request, 
            YapeNotificationResponse decryptedResponse, 
            YapeNotificationAuditEntity auditRecord) {
        
        return updateAuditWithDecryptedData(auditRecord, decryptedResponse)
                .chain(updatedAudit -> processPaymentNotification(request, decryptedResponse)
                        .chain(paymentResponse -> finalizeAuditAndCreateResponse(updatedAudit, paymentResponse, decryptedResponse)));
    }

    private Uni<YapeNotificationAuditEntity> updateAuditWithDecryptedData(YapeNotificationAuditEntity auditRecord,
                                                                          YapeNotificationResponse decryptedResponse) {
        auditRecord.decryptionStatus = "SUCCESS";
        auditRecord.extractedAmount = decryptedResponse.amount();
        auditRecord.extractedSenderName = decryptedResponse.senderName();
        auditRecord.extractedYapeCode = decryptedResponse.transactionId().replace("YAPE_", "");
        auditRecord.transactionId = decryptedResponse.transactionId();

        return yapeNotificationAuditRepository.persist(auditRecord);
    }

    private Uni<PaymentNotificationResponse> processPaymentNotification(
            YapeNotificationRequest request,
            YapeNotificationResponse decryptedResponse) {
        
        PaymentNotificationRequest paymentRequest = new PaymentNotificationRequest(
            request.adminId(),
            decryptedResponse.amount(),
            decryptedResponse.senderName(),
            decryptedResponse.transactionId(),
            request.deduplicationHash()
        );

        return paymentNotificationService.processPaymentNotification(paymentRequest);
    }

    private Uni<ApiResponse<YapeNotificationResponse>> finalizeAuditAndCreateResponse(
            YapeNotificationAuditEntity auditRecord,
            PaymentNotificationResponse paymentResponse, 
            YapeNotificationResponse decryptedResponse) {
        
        auditRecord.paymentNotificationId = paymentResponse.paymentId();
        
        return yapeNotificationAuditRepository.persist(auditRecord)
                .map(updatedAudit -> {
                    YapeNotificationResponse yapeResponse = new YapeNotificationResponse(
                        paymentResponse.paymentId(),
                        decryptedResponse.transactionId(),
                        decryptedResponse.amount(),
                        decryptedResponse.senderPhone(),
                        decryptedResponse.senderName(),
                        decryptedResponse.receiverPhone(),
                        "PENDING_CONFIRMATION",
                        paymentResponse.timestamp(),
                        "Transaction processed and sent to sellers for confirmation"
                    );

                    return ApiResponse.success("Yape notification processed successfully", yapeResponse);
                });
    }
}
