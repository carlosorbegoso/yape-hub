package org.sky.service.hubnotifications;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.ApiResponse;
import org.sky.dto.payment.PaymentClaimRequest;
import org.sky.dto.payment.PaymentRejectRequest;
import org.sky.dto.payment.PendingPaymentsResponse;
import org.sky.dto.payment.AdminPaymentManagementResponse;
import org.sky.dto.payment.PaymentNotificationResponse;
import org.sky.service.security.SecurityService;
import org.sky.service.websocket.WebSocketNotificationService;
import org.sky.service.hubnotifications.PaymentNotificationService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

@ApplicationScoped
public class PaymentControllerService {

    @Inject
    PaymentNotificationService paymentNotificationService;

    @Inject
    SecurityService securityService;

    @Inject
    WebSocketNotificationService webSocketNotificationService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public Uni<DateRange> validateAndParseDates(String startDateStr, String endDateStr) {
        try {
            LocalDate startDate, endDate;
            if (startDateStr != null && endDateStr != null) {
                startDate = LocalDate.parse(startDateStr, DATE_FORMATTER);
                endDate = LocalDate.parse(endDateStr, DATE_FORMATTER);
            } else {
                // Default: last month
                endDate = LocalDate.now();
                startDate = endDate.minusDays(30);
            }
            return Uni.createFrom().item(new DateRange(startDate, endDate));
        } catch (DateTimeParseException e) {
            return Uni.createFrom().failure(new IllegalArgumentException("Invalid date format. Use yyyy-MM-dd"));
        }
    }

    public Uni<Map<String, Object>> getSellerConnectionStatus(Long sellerId, String authorization) {
        return securityService.validateSellerAuthorization(authorization, sellerId)
                .chain(userId -> {
                    boolean isConnected = webSocketNotificationService.isSellerConnected(sellerId);
                    int totalConnections = webSocketNotificationService.getConnectedSellersCount();
                    
                    Map<String, Object> status = Map.of(
                        "sellerId", sellerId,
                        "isConnected", isConnected,
                        "totalConnectedSellers", totalConnections,
                        "timestamp", java.time.LocalDateTime.now()
                    );
                    
                    return Uni.createFrom().item(status);
                });
    }

    public Uni<PaymentNotificationResponse> claimPayment(PaymentClaimRequest request, String authorization) {
        return securityService.validateSellerAuthorization(authorization, request.sellerId())
                .chain(userId -> paymentNotificationService.claimPayment(request.paymentId()));
    }

    public Uni<PaymentNotificationResponse> rejectPayment(PaymentRejectRequest request, String authorization) {
        return securityService.validateSellerAuthorization(authorization, request.sellerId())
                .chain(userId -> paymentNotificationService.rejectPayment(request.paymentId(), request.reason()));
    }

    public Uni<PendingPaymentsResponse> getPendingPayments(Long sellerId, Long adminId, 
                                                          String startDateStr, String endDateStr,
                                                          int page, int size, int limit, 
                                                          String authorization) {
        return validateAndParseDates(startDateStr, endDateStr)
                .chain(dateRange -> {
                    final int finalSize = (size == 20 && limit != 20) ? limit : size;
                    
                    return securityService.validateJwtToken(authorization)
                            .chain(userId -> {
                                // If sellerId is null, only allow for ADMINs
                                if (sellerId == null) {
                                    return securityService.validateAdminAuthorization(authorization, userId)
                                            .chain(adminUserId -> paymentNotificationService.getAllPendingPayments(
                                                    page, finalSize, dateRange.startDate, dateRange.endDate))
                                            .onItem().transform(adminResponse -> {
                                                // Convert AdminPaymentManagementResponse to PendingPaymentsResponse
                                                return new PendingPaymentsResponse(
                                                    adminResponse.payments().stream()
                                                        .map(p -> new org.sky.dto.payment.PaymentNotificationResponse(
                                                            p.paymentId(), p.amount(), p.senderName(), 
                                                            p.yapeCode(), p.status(), p.createdAt(), "Pending payment"
                                                        )).toList(),
                                                    new PendingPaymentsResponse.PaginationInfo(
                                                        adminResponse.pagination().currentPage(),
                                                        adminResponse.pagination().totalPages(),
                                                        adminResponse.pagination().totalItems(),
                                                        adminResponse.pagination().itemsPerPage()
                                                    )
                                                );
                                            });
                                }
                                
                                // If adminId is provided, validate that it's admin
                                if (adminId != null) {
                                    return securityService.validateAdminAuthorization(authorization, adminId)
                                            .chain(adminUserId -> paymentNotificationService.getPendingPaymentsForSeller(
                                                    sellerId, page, finalSize, dateRange.startDate, dateRange.endDate));
                                }
                                
                                // Validate that the seller can see their own payments
                                return securityService.validateSellerAuthorization(authorization, sellerId)
                                        .chain(sellerUserId -> paymentNotificationService.getPendingPaymentsForSeller(
                                                sellerId, page, finalSize, dateRange.startDate, dateRange.endDate));
                            });
                });
    }

    public Uni<AdminPaymentManagementResponse> getAdminPaymentManagement(Long adminId, int page, int size, 
                                                                        String status, String startDateStr, 
                                                                        String endDateStr, String authorization) {
        return validateAndParseDates(startDateStr, endDateStr)
                .chain(dateRange -> securityService.validateAdminAuthorization(authorization, adminId)
                        .chain(userId -> paymentNotificationService.getAdminPaymentManagement(
                                adminId, page, size, status, dateRange.startDate, dateRange.endDate)));
    }

    public Uni<Map<String, Object>> getNotificationQueueStats(String authorization) {
        return securityService.validateJwtToken(authorization)
                .chain(userId -> {
                    Map<String, Object> stats = paymentNotificationService.getNotificationQueueStats();
                    return Uni.createFrom().item(stats);
                });
    }

    public Uni<Map<Long, Boolean>> getConnectedSellersForAdmin(Long adminId, String authorization) {
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> paymentNotificationService.getConnectedSellersForAdmin(adminId));
    }

    public Uni<java.util.List<org.sky.model.SellerEntity>> getAllSellersStatusForAdmin(Long adminId, String authorization) {
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> paymentNotificationService.getAllSellersStatusForAdmin(adminId));
    }

    public Uni<AdminPaymentManagementResponse> getConfirmedPaymentsForSeller(Long sellerId, int page, int size,
                                                                            String startDateStr, String endDateStr,
                                                                            String authorization) {
        return validateAndParseDates(startDateStr, endDateStr)
                .chain(dateRange -> securityService.validateSellerAuthorization(authorization, sellerId)
                        .chain(userId -> paymentNotificationService.getConfirmedPaymentsForSellerPaginated(
                                sellerId, page, size, dateRange.startDate, dateRange.endDate)));
    }

    public record DateRange(LocalDate startDate, LocalDate endDate) {}
}
