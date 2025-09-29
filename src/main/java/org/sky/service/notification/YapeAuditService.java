package org.sky.service.notification;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import org.sky.dto.ApiResponse;
import org.sky.dto.notification.YapeAuditResponse;
import org.sky.model.YapeNotificationAudit;
import org.sky.repository.YapeNotificationAuditRepository;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class YapeAuditService {

    @Inject
    YapeNotificationAuditRepository yapeNotificationAuditRepository;

    @WithTransaction
    public Uni<ApiResponse<List<YapeAuditResponse>>> getYapeNotificationAudit(Long adminId, int page, int size) {
        final int validatedPage = Math.max(0, page);
        final int validatedSize = (size <= 0 || size > 100) ? 20 : size;
        
        return yapeNotificationAuditRepository.findByAdminId(adminId)
                .map(auditRecords -> {
                    List<YapeAuditResponse> paginatedResponses = paginateAndConvert(auditRecords, validatedPage, validatedSize);
                    return ApiResponse.success("Yape audit retrieved successfully", paginatedResponses);
                });
    }

    private List<YapeAuditResponse> paginateAndConvert(List<YapeNotificationAudit> auditRecords, int page, int size) {
        int totalCount = auditRecords.size();
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalCount);
        
        List<YapeNotificationAudit> paginatedRecords = auditRecords.subList(startIndex, endIndex);
        
        return paginatedRecords.stream()
                .map(this::convertToResponse)
                .toList();
    }

    private YapeAuditResponse convertToResponse(YapeNotificationAudit audit) {
        return new YapeAuditResponse(
            audit.id,
            audit.adminId,
            audit.encryptedNotification,
            audit.deviceFingerprint,
            audit.timestamp,
            audit.deduplicationHash,
            audit.decryptionStatus,
            audit.decryptionError,
            audit.extractedAmount,
            audit.extractedSenderName,
            audit.extractedYapeCode,
            audit.transactionId,
            audit.paymentNotificationId,
            audit.createdAt,
            audit.updatedAt
        );
    }
}
