package org.sky.service.hubnotifications;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.sky.dto.request.payment.PaymentClaimRequest;
import org.sky.dto.request.payment.PaymentRejectRequest;
import org.sky.dto.response.payment.PendingPaymentsResponse;
import org.sky.dto.response.admin.AdminPaymentManagementResponse;
import org.sky.dto.response.payment.PaymentNotificationResponse;
import org.sky.service.websocket.WebSocketNotificationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@ApplicationScoped
public class HubNotificationControllerService {

    @Inject
    PaymentNotificationService paymentNotificationService;

    @Inject
    WebSocketNotificationService webSocketNotificationService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ==================================================================================
    // BUSINESS LOGIC METHODS - SIN VALIDACIONES DE SEGURIDAD (debe hacerse en Controller)
    // ==================================================================================

    public record DateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {}

    public Uni<DateRange> validateAndParseDates(String startDateStr, String endDateStr) {
        try {
            LocalDate startDate, endDate;
            if (startDateStr != null && endDateStr != null) {
                startDate = LocalDate.parse(startDateStr, DATE_FORMATTER);
                endDate = LocalDate.parse(endDateStr, DATE_FORMATTER);
            } else {
                // Use current month as default
                LocalDate now = LocalDate.now();
                startDate = now.withDayOfMonth(1);
                endDate = now.withDayOfMonth(now.lengthOfMonth());
            }
            return Uni.createFrom().item(new DateRange(startDate, endDate));
        } catch (DateTimeParseException e) {
            return Uni.createFrom().failure(new IllegalArgumentException("Invalid date format. Use yyyy-MM-dd"));
        }
    }

    public Uni<java.util.Map<String, Object>> getSellerConnectionStatus(Long sellerId, Long adminId) {
        // Simplified implementation - in real scenario you'd check WebSocket connections
        java.util.Map<String, Object> status = java.util.Map.of(
            "sellerId", sellerId,
            "isConnected", true, // Simplified for now
            "lastSeen", java.time.LocalDateTime.now(),
            "websocketEndpoint", "/ws/payments/" + sellerId
        );
        return Uni.createFrom().item(status);
    }

    public Uni<PaymentNotificationResponse> claimPayment(PaymentClaimRequest request, Long adminId, Long sellerId) {
        return paymentNotificationService.claimPayment(request.paymentId());
    }

    public Uni<PaymentNotificationResponse> rejectPayment(PaymentRejectRequest request, Long adminId, Long sellerId) {
        return paymentNotificationService.rejectPayment(request.paymentId(), request.reason());
    }

    public Uni<PendingPaymentsResponse> getPendingPayments(Long sellerId, Long adminId, String startDateStr, String endDateStr, int page, int size, int limit) {
        int effectiveSize = Math.min(size, limit);
        
        return validateAndParseDates(startDateStr, endDateStr)
                .chain(dateRange -> paymentNotificationService.getPendingPaymentsForSeller(
                    sellerId, page, effectiveSize, dateRange.startDate(), dateRange.endDate()));
    }

    public Uni<java.util.Map<String, Object>> getConnectedSellersForAdmin(Long adminId) {
        return paymentNotificationService.getConnectedSellersForAdmin(adminId).onItem().transform(connectionMap -> {
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("adminId", adminId);
            result.put("connectedSellers", connectionMap);
            result.put("timestamp", java.time.LocalDateTime.now());
            return result;
        });
    }

    public Uni<java.util.Map<String, Object>> getAllSellersStatusForAdmin(Long adminId) {
        return paymentNotificationService.getAllSellersStatusForAdmin(adminId)
                .chain(sellers -> {
                    java.util.List<java.util.Map<String, Object>> simpleSellers = sellers.stream()
                        .map(seller -> {
                            java.util.Map<String, Object> sellerInfo = new java.util.HashMap<>();
                            sellerInfo.put("sellerId", seller.id);
                            sellerInfo.put("name", seller.sellerName != null ? seller.sellerName : "N/A");
                            sellerInfo.put("email", seller.email != null ? seller.email : "N/A");
                            sellerInfo.put("phone", seller.phone != null ? seller.phone : "N/A");
                            sellerInfo.put("isActive", seller.isActive);
                            sellerInfo.put("isOnline", seller.isOnline != null ? seller.isOnline : false);
                            sellerInfo.put("totalPayments", seller.totalPayments != null ? seller.totalPayments : 0);
                            sellerInfo.put("totalAmount", seller.totalAmount != null ? seller.totalAmount : BigDecimal.ZERO);
                            return sellerInfo;
                        })
                        .toList();
                    
                    java.util.Map<String, Object> result = java.util.Map.of(
                        "adminId", adminId,
                        "sellers", simpleSellers,
                        "totalSellers", sellers.size(),
                        "activeCount", sellers.stream().mapToInt(s -> (s.isActive != null && s.isActive) ? 1 : 0).sum(),
                        "timestamp", java.time.LocalDateTime.now()
                    );
                    
                    return Uni.createFrom().item(result);
                });
    }

    public Uni<AdminPaymentManagementResponse> getPendingPaymentsForAdmin(Long adminId, Long sellerId, String startDateStr, String endDateStr, int page, int size, int limit) {
        int effectiveSize = Math.min(size, limit);
        
        return validateAndParseDates(startDateStr, endDateStr)
                .chain(dateRange -> paymentNotificationService.getPendingPaymentsForSeller(
                    sellerId, page, effectiveSize, dateRange.startDate(), dateRange.endDate()))
                .chain(pendingResponse -> 
                    validateAndParseDates(startDateStr, endDateStr)
                        .chain(dates -> paymentNotificationService.getAdminPaymentManagement(adminId, page, effectiveSize, "PENDING", dates.startDate(), dates.endDate()))
                );
    }

    public Uni<AdminPaymentManagementResponse> getPendingPaymentsForSeller(Long sellerId, Long adminId, String startDateStr, String endDateStr, int page, int size, int limit) {
        int effectiveSize = Math.min(size, limit);
        
        return validateAndParseDates(startDateStr, endDateStr)
                .chain(dateRange -> paymentNotificationService.getConfirmedPaymentsForSellerPaginated(sellerId, page, effectiveSize, dateRange.startDate(), dateRange.endDate()));
    }

    public Uni<AdminPaymentManagementResponse> getAdminPaymentManagement(Long adminId, int page, int size, String status, String startDateStr, String endDateStr) {
        return validateAndParseDates(startDateStr, endDateStr)
                .chain(dateRange -> paymentNotificationService.getAdminPaymentManagement(adminId, page, size, status, dateRange.startDate(), dateRange.endDate()));
    }

    public Uni<AdminPaymentManagementResponse> getConfirmedPaymentsForSeller(Long sellerId, Long adminId, String startDateStr, String endDateStr) {
        return paymentNotificationService.getConfirmedPaymentsForSellerPaginated(sellerId, 0, 20, null, null);
    }
}
