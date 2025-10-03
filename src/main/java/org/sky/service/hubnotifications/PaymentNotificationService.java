package org.sky.service.hubnotifications;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.payment.PaymentNotificationRequest;
import org.sky.dto.payment.PaymentNotificationResponse;
import org.sky.dto.payment.PendingPaymentsResponse;
import org.sky.dto.payment.AdminPaymentManagementResponse;
import org.sky.model.PaymentNotificationEntity;
import org.sky.model.PaymentRejectionEntity;
import org.sky.model.SellerEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class PaymentNotificationService {

    @Inject
    PaymentNotificationDataService dataService;

    @Inject
    PaymentNotificationProcessor processor;


    public Uni<PaymentNotificationResponse> processPaymentNotification(PaymentNotificationRequest request) {
        return PaymentNotificationValidator.validateRequest().apply(request)
            .chain(validRequest -> PaymentNotificationValidator.validateAdminId().apply(request.adminId()))
            .chain(adminId -> dataService.findSellersByAdminId(adminId))
            .chain(sellers -> {
                if (sellers.isEmpty()) {
                    return Uni.createFrom().failure(new RuntimeException("No sellers found for admin"));
                }
                // Send notification to ALL sellers of the admin
                return sendNotificationToAllSellers(request, sellers);
            });
    }



    public Uni<PendingPaymentsResponse> getPendingPaymentsForSeller(Long sellerId, int page, int size, LocalDate startDate, LocalDate endDate) {
        return PaymentNotificationValidator.validateSellerId().apply(sellerId)
            .chain(validSellerId -> {
                Map<String, Object> params = Map.of(
                    "page", page,
                    "size", size,
                    "startDate", startDate,
                    "endDate", endDate
                );
                return PaymentNotificationValidator.validatePagination().apply(params)
                    .chain(PaymentNotificationValidator.validateDateRange()::apply);
            })
            .chain(validParams -> {
                int validPage = (Integer) validParams.get("page");
                int validSize = (Integer) validParams.get("size");
                LocalDate validStartDate = (LocalDate) validParams.get("startDate");
                LocalDate validEndDate = (LocalDate) validParams.get("endDate");
                
                return Uni.combine().all().unis(
                    dataService.findPendingPaymentsForSeller(sellerId, validPage, validSize, validStartDate, validEndDate),
                    dataService.countPendingPaymentsForSeller(sellerId, validStartDate, validEndDate)
                ).asTuple();
            })
            .chain(tuple -> {
                List<PaymentNotificationEntity> payments = tuple.getItem1();
                Long totalCount = tuple.getItem2();
                
                List<PaymentNotificationResponse> responses = payments.stream()
                    .map(PaymentNotificationMapper.ENTITY_TO_RESPONSE)
                    .toList();
                
                return Uni.createFrom().item(new PendingPaymentsResponse(
                    responses,
                    new PendingPaymentsResponse.PaginationInfo(
                        page,
                        (int) Math.ceil((double) totalCount / size),
                        totalCount,
                        size
                    )
                ));
            });
    }


    public Uni<AdminPaymentManagementResponse> getAllPendingPayments(int page, int size, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> params = Map.of(
            "page", page,
            "size", size,
            "startDate", startDate,
            "endDate", endDate
        );
        
        return PaymentNotificationValidator.validatePagination().apply(params)
            .chain(PaymentNotificationValidator.validateDateRange()::apply)
            .chain(validParams -> {
                int validPage = (Integer) validParams.get("page");
                int validSize = (Integer) validParams.get("size");
                LocalDate validStartDate = (LocalDate) validParams.get("startDate");
                LocalDate validEndDate = (LocalDate) validParams.get("endDate");
                
                return Uni.combine().all().unis(
                    dataService.findAllPendingPayments(validPage, validSize, validStartDate, validEndDate),
                    dataService.countAllPendingPayments(validStartDate, validEndDate)
                ).asTuple();
            })
            .chain(tuple -> {
                List<PaymentNotificationEntity> payments = tuple.getItem1();
                Long totalCount = tuple.getItem2();
                
                List<PaymentNotificationResponse> responses = payments.stream()
                    .map(PaymentNotificationMapper.ENTITY_TO_RESPONSE)
                    .toList();
                
                return Uni.createFrom().item(new AdminPaymentManagementResponse(
                    responses.stream().map(r -> new AdminPaymentManagementResponse.PaymentDetail(
                        r.paymentId(),
                        r.amount(),
                        r.senderName(),
                        r.yapeCode(),
                        r.status(),
                        r.timestamp(),
                        null, // confirmedBy
                        null, // confirmedAt
                        null, // rejectedBy
                        null, // rejectedAt
                        null, // rejectionReason
                        "Seller Name", // sellerName - would need to be fetched
                        "Branch Name"  // branchName - would need to be fetched
                    )).toList(),
                    new AdminPaymentManagementResponse.PaymentSummary(
                        totalCount,
                        totalCount, // pendingCount
                        0L, // confirmedCount
                        0L, // rejectedCount
                        responses.stream().mapToDouble(PaymentNotificationResponse::amount).sum(), // totalAmount
                        0.0, // confirmedAmount
                        responses.stream().mapToDouble(PaymentNotificationResponse::amount).sum() // pendingAmount
                    ),
                    new AdminPaymentManagementResponse.PaginationInfo(
                        page,
                        (int) Math.ceil((double) totalCount / size),
                        totalCount,
                        size
                    )
                ));
            });
    }


    public Uni<PaymentNotificationResponse> claimPayment(Long paymentId) {
        return PaymentNotificationValidator.validatePaymentId().apply(paymentId)
            .chain(validPaymentId -> dataService.findPaymentById(validPaymentId))
            .chain(payment -> {
                if (payment == null) {
                    return Uni.createFrom().failure(org.sky.exception.ValidationException.requiredField("payment"));
                }
                if (!"PENDING".equals(payment.status)) {
                    return Uni.createFrom().failure(org.sky.exception.ValidationException.invalidField("payment", payment.id.toString(), "is not pending"));
                }
                return dataService.updatePaymentStatus(paymentId, "CLAIMED");
            })
            .onItem().transform(PaymentNotificationMapper.ENTITY_TO_RESPONSE);
    }


    public Uni<PaymentNotificationResponse> rejectPayment(Long paymentId, String reason) {
        return PaymentNotificationValidator.validatePaymentId().apply(paymentId)
            .chain(validPaymentId -> dataService.findPaymentById(validPaymentId))
            .chain(payment -> {
                if (payment == null) {
                    return Uni.createFrom().failure(org.sky.exception.ValidationException.requiredField("payment"));
                }
                if (!"PENDING".equals(payment.status)) {
                    return Uni.createFrom().failure(org.sky.exception.ValidationException.invalidField("payment", payment.id.toString(), "is not pending"));
                }
                
                PaymentRejectionEntity rejection = new PaymentRejectionEntity(
                    payment.id,
                    payment.adminId, // Using adminId as sellerId for now
                    reason
                );
                
                return dataService.savePaymentRejection(rejection)
                    .chain(r -> dataService.updatePaymentStatus(paymentId, "REJECTED"));
            })
            .onItem().transform(PaymentNotificationMapper.ENTITY_TO_RESPONSE);
    }


    private Uni<PaymentNotificationResponse> sendNotificationToAllSellers(PaymentNotificationRequest request, List<SellerEntity> sellers) {
        // Create notification for each seller asynchronously
        List<Uni<Void>> notificationTasks = sellers.stream()
            .map(seller -> createNotificationForSeller(request, seller).replaceWithVoid())
            .toList();
        
        // Wait for all notifications to complete
        return Uni.combine().all().unis(notificationTasks)
            .discardItems() // Wait for all tasks to complete
            .chain(v -> {
                // Return the first seller's notification as the main response
                return createNotificationForSeller(request, sellers.getFirst());
            });
    }

    private Uni<PaymentNotificationResponse> createNotificationForSeller(PaymentNotificationRequest request, SellerEntity seller) {
        return PaymentNotificationValidator.validateSeller().apply(seller)
            .chain(validSeller -> dataService.createPaymentForSeller(request, validSeller))
            .chain(savedPayment -> {
                PaymentNotificationResponse response = PaymentNotificationMapper.ENTITY_TO_RESPONSE.apply(savedPayment);
                return processor.addToQueue(seller.id, response)
                    .onItem().transform(v -> response);
            });
    }

    public Uni<AdminPaymentManagementResponse> getAdminPaymentManagement(Long adminId, int page, int size, String status, LocalDate startDate, LocalDate endDate) {
        return getAllPendingPayments(page, size, startDate, endDate);
    }

    public Map<String, Object> getNotificationQueueStats() {
        return Map.of(
            "processedCount", processor.getProcessedCount(),
            "timestamp", java.time.LocalDateTime.now()
        );
    }

    public Uni<Map<Long, Boolean>> getConnectedSellersForAdmin(Long adminId) {
        return dataService.findSellersByAdminId(adminId)
            .onItem().transform(sellers -> {
                Map<Long, Boolean> result = new java.util.HashMap<>();
                for (SellerEntity seller : sellers) {
                    result.put(seller.id, true); // Simplified for now
                }
                return result;
            });
    }

    public Uni<List<SellerEntity>> getAllSellersStatusForAdmin(Long adminId) {
        return dataService.findSellersByAdminId(adminId);
    }

    public Uni<AdminPaymentManagementResponse> getConfirmedPaymentsForSellerPaginated(Long sellerId, int page, int size, LocalDate startDate, LocalDate endDate) {
        return getAllPendingPayments(page, size, startDate, endDate);
    }
}
