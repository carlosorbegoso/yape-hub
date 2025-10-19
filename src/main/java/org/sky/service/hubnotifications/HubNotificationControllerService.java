package org.sky.service.hubnotifications;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.sky.dto.request.payment.PaymentClaimRequest;
import org.sky.dto.request.payment.PaymentRejectRequest;
import org.sky.dto.response.admin.AdminPaymentManagementResponse;
import org.sky.dto.response.payment.PaymentNotificationResponse;
import org.sky.repository.SellerRepository;
import org.jboss.logging.Logger;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@ApplicationScoped
public class HubNotificationControllerService {

    private static final Logger log = Logger.getLogger(HubNotificationControllerService.class);

    @Inject
    PaymentNotificationService paymentNotificationService;


    @Inject
    SellerRepository sellerRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    public record DateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {}

    public Uni<DateRange> validateAndParseDates(String startDateStr, String endDateStr) {
        try {
            LocalDate startDate, endDate;
            if (startDateStr != null && endDateStr != null) {
                startDate = LocalDate.parse(startDateStr, DATE_FORMATTER);
                endDate = LocalDate.parse(endDateStr, DATE_FORMATTER);
            } else {
                // Use broader default range to include more historical data
                LocalDate now = LocalDate.now();
                startDate = now.minusMonths(6); // Last 6 months
                endDate = now.plusDays(1); // Include today
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


    @WithTransaction
    public Uni<AdminPaymentManagementResponse> getPaymentsByRoleAndStatus(Long userId, Long sellerId, String status, String startDateStr, String endDateStr, int page, int size, int limit) {
        int effectiveSize = Math.min(size, limit);
        
        log.info("🔍 Getting payments by role and status for userId: " + userId + ", sellerId: " + sellerId + ", status: " + status);
        
        return validateAndParseDates(startDateStr, endDateStr)
                .chain(dateRange -> {
                    log.info("📅 Date range parsed: " + dateRange.startDate() + " to " + dateRange.endDate());
                    
                    // Determinar el rol del usuario
                    return paymentNotificationService.getUserRole(userId)
                            .chain(userRole -> {
                                log.info("✅ User " + userId + " has role: " + userRole);
                                if ("ADMIN".equals(userRole)) {
                                    // Admin: obtener todos los pagos de sus vendedores
                                    log.info("📊 Getting payments for admin: " + userId + " with status: " + status);
                                    return paymentNotificationService.getPaymentsForAdminByStatus(userId, page, effectiveSize, status, dateRange.startDate(), dateRange.endDate());
                                } else if ("SELLER".equals(userRole)) {
                                    // Seller: obtener solo sus propios pagos
                                    // Validar que el seller no pueda usar status=ALL
                                    if ("ALL".equalsIgnoreCase(status)) {
                                        log.error("❌ Seller " + userId + " attempted to access ALL payments - denied");
                                        return Uni.createFrom().failure(new SecurityException("Los vendedores no pueden acceder a todos los pagos. Use PENDING, CLAIMED o REJECTED."));
                                    }
                                    
                                    // Validar que los estados solicitados sean válidos para sellers
                                    String[] requestedStatuses = status.split(",");
                                    for (String requestedStatus : requestedStatuses) {
                                        String trimmedStatus = requestedStatus.trim().toUpperCase();
                                        if (!"PENDING".equals(trimmedStatus) && !"CLAIMED".equals(trimmedStatus) && !"REJECTED".equals(trimmedStatus)) {
                                            log.error("❌ Seller " + userId + " attempted to access invalid status: " + trimmedStatus);
                                            return Uni.createFrom().failure(new SecurityException("Estado no válido para vendedores: " + trimmedStatus + ". Use PENDING, CLAIMED o REJECTED."));
                                        }
                                    }
                                    
                                    // Necesitamos obtener el adminId del seller
                                    log.info("🔍 Looking up seller for userId: " + userId);
                                    return sellerRepository.findByUserId(userId)
                                            .chain(seller -> {
                                                if (seller == null) {
                                                    log.error("❌ Seller not found for userId: " + userId);
                                                    return Uni.createFrom().failure(new SecurityException("Vendedor no encontrado para el usuario: " + userId));
                                                }
                                                log.info("✅ Found seller: " + seller.id + " for userId: " + userId);
                                                // Los sellers ven los pagos de su admin (pero no ALL)
                                                return paymentNotificationService.getPaymentsForAdminByStatus(seller.branch.admin.id, page, effectiveSize, status, dateRange.startDate(), dateRange.endDate());
                                            });
                                } else {
                                    log.error("❌ Invalid user role: " + userRole + " for userId: " + userId);
                                    return Uni.createFrom().failure(new SecurityException("Rol de usuario no válido: " + userRole));
                                }
                            });
                });
    }

}
