package org.sky.service.hubnotifications;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import org.sky.dto.request.payment.PaymentNotificationRequest;
import org.sky.dto.response.common.PaginationInfo;
import org.sky.dto.response.payment.PaymentDetail;
import org.sky.dto.response.payment.PaymentNotificationResponse;
import org.sky.dto.response.payment.PaymentSummary;
import org.sky.dto.response.payment.PendingPaymentsResponse;
import org.sky.dto.response.admin.AdminPaymentManagementResponse;
import org.sky.model.PaymentNotificationEntity;
import org.sky.model.PaymentRejectionEntity;
import org.sky.model.SellerEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.sky.service.hubnotifications.PaymentNotificationDataService;
import org.sky.service.hubnotifications.PaymentNotificationMapper;
import org.sky.service.hubnotifications.PaymentNotificationProcessor;
import org.sky.service.hubnotifications.PaymentNotificationValidator;
import org.sky.service.security.SecurityService;
import org.sky.service.websocket.WebSocketNotificationService;
import org.sky.dto.response.seller.ConnectedSellerInfo;
import org.sky.dto.response.seller.ConnectedSellersResponse;

@ApplicationScoped
public class PaymentNotificationService {

    private static final Logger log = Logger.getLogger(PaymentNotificationService.class);

    @Inject
    PaymentNotificationDataService dataService;

    @Inject
    PaymentNotificationProcessor processor;

    @Inject
    SecurityService securityService;

    @Inject
    PaymentNotificationMapper paymentNotificationMapper;

    @Inject
    WebSocketNotificationService webSocketNotificationService;


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
                    new PaginationInfo(
                        page,
                        (int) Math.ceil((double) totalCount / size),
                        totalCount,
                        size,
                        page < (int) Math.ceil((double) totalCount / size),
                        page > 1
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
                    responses.stream().map(r -> new PaymentDetail(
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
                    new PaymentSummary(
                        totalCount,
                        totalCount, // pendingCount
                        0L, // confirmedCount
                        0L, // rejectedCount
                        responses.stream().mapToDouble(PaymentNotificationResponse::amount).sum(), // totalAmount
                        0.0, // confirmedAmount
                        responses.stream().mapToDouble(PaymentNotificationResponse::amount).sum() // pendingAmount
                    ),
                    new PaginationInfo(
                        page,
                        (int) Math.ceil((double) totalCount / size),
                        totalCount,
                        size,
                        page < (int) Math.ceil((double) totalCount / size),
                        page > 1
                    )
                ));
            });
    }


    @WithTransaction
    public Uni<PaymentNotificationResponse> claimPayment(Long paymentId) {
        log.info("🔍 Attempting to claim payment with ID: " + paymentId);
        
        return PaymentNotificationValidator.validatePaymentId().apply(paymentId)
            .chain(validPaymentId -> {
                log.info("✅ Payment ID validation passed: " + validPaymentId);
                return dataService.findPaymentById(validPaymentId);
            })
            .chain(payment -> {
                if (payment == null) {
                    log.warn("❌ Payment not found with ID: " + paymentId);
                    return Uni.createFrom().failure(org.sky.exception.ValidationException.requiredField("payment"));
                }
                
                log.info("📊 Found payment: ID=" + payment.id + ", status=" + payment.status + 
                        ", amount=" + payment.amount + ", adminId=" + payment.adminId);
                
                if (!"PENDING".equals(payment.status)) {
                    log.warn("❌ Payment is not pending: ID=" + payment.id + ", status=" + payment.status);
                    return Uni.createFrom().failure(org.sky.exception.ValidationException.invalidField("payment", payment.id.toString(), "is not pending"));
                }
                
                log.info("✅ Payment is pending, proceeding to claim");
                return dataService.updatePaymentStatus(paymentId, "CLAIMED");
            })
            .onItem().transform(payment -> {
                log.info("🎉 Payment claimed successfully: ID=" + payment.id);
                return PaymentNotificationMapper.ENTITY_TO_RESPONSE.apply(payment);
            })
            .onFailure().invoke(throwable -> {
                log.error("❌ Error claiming payment: " + throwable.getMessage());
            });
    }


    @WithTransaction
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

    @WithTransaction
    public Uni<AdminPaymentManagementResponse> getAdminPaymentManagement(Long adminId, int page, int size, String status, LocalDate startDate, LocalDate endDate) {
        log.info("🔍 Getting admin payment management for adminId: " + adminId + ", status: " + status);
        
        return dataService.findPaymentsForAdminByStatus(adminId, page, size, status, startDate, endDate)
            .chain(payments -> {
                log.info("📊 Found " + payments.size() + " payments for admin: " + adminId + " with status: " + status);
                
                // Obtener estadísticas del total de pagos del admin
                return dataService.countPaymentsForAdminByStatus(adminId, null, startDate, endDate)
                    .chain(totalCount -> 
                        dataService.countPaymentsForAdminByStatus(adminId, "PENDING", startDate, endDate)
                            .chain(pendingCount ->
                                dataService.countPaymentsForAdminByStatus(adminId, "CONFIRMED", startDate, endDate)
                                    .chain(confirmedCount ->
                                        dataService.countPaymentsForAdminByStatus(adminId, "REJECTED", startDate, endDate)
                                            .map(rejectedCount -> {
                                                List<PaymentNotificationResponse> responses = payments.stream()
                                                    .map(PaymentNotificationMapper.ENTITY_TO_RESPONSE)
                                                    .collect(Collectors.toList());
                                                
                                                // Calcular montos solo de la página actual para mostrar
                                                double totalAmount = responses.stream().mapToDouble(PaymentNotificationResponse::amount).sum();
                                                double confirmedAmount = responses.stream()
                                                    .filter(r -> "CONFIRMED".equals(r.status()))
                                                    .mapToDouble(PaymentNotificationResponse::amount).sum();
                                                double pendingAmount = responses.stream()
                                                    .filter(r -> "PENDING".equals(r.status()))
                                                    .mapToDouble(PaymentNotificationResponse::amount).sum();
                                                
                                                return new AdminPaymentManagementResponse(
                                                    responses.stream().map(r -> new PaymentDetail(
                                                        r.paymentId(),
                                                        r.amount(),
                                                        r.senderName(),
                                                        r.yapeCode(),
                                                        r.status(),
                                                        r.timestamp(),
                                                        null, // confirmedBy - would need to be fetched from entity
                                                        null, // confirmedAt - would need to be fetched from entity
                                                        null, // rejectedBy - would need to be fetched from entity
                                                        null, // rejectedAt - would need to be fetched from entity
                                                        null, // rejectionReason - would need to be fetched from entity
                                                        "Seller Name", // sellerName - would need to be fetched
                                                        "Branch Name"  // branchName - would need to be fetched
                                                    )).collect(Collectors.toList()),
                                                    new PaymentSummary(
                                                        totalCount,
                                                        pendingCount,
                                                        confirmedCount,
                                                        rejectedCount,
                                                        totalAmount,
                                                        confirmedAmount,
                                                        pendingAmount
                                                    ),
                                                    new PaginationInfo(
                                                        page,
                                                        (int) Math.ceil((double) totalCount / size),
                                                        totalCount,
                                                        size,
                                                        page < (int) Math.ceil((double) totalCount / size) - 1,
                                                        page > 0
                                                    )
                                                );
                                            })
                                    )
                            )
                    );
            })
            .onFailure().recoverWithItem(throwable -> {
                log.error("❌ Error getting admin payment management: " + throwable.getMessage());
                return new AdminPaymentManagementResponse(
                    List.of(), 
                    PaymentSummary.empty(), 
                    new PaginationInfo(page, 0, 0, size, false, false)
                );
            });
    }

    public Map<String, Object> getNotificationQueueStats() {
        return Map.of(
            "processedCount", processor.getProcessedCount(),
            "timestamp", java.time.LocalDateTime.now()
        );
    }

    /**
     * Obtiene vendedores conectados con información completa incluyendo estado WebSocket
     */
    @WithTransaction
    public Uni<ConnectedSellersResponse> getConnectedSellersForAdmin(Long adminId) {
        log.info("🔍 Getting connected sellers for admin: " + adminId);
        
        return dataService.findSellersByAdminId(adminId)
            .chain(sellers -> {
                log.info("📊 Found " + sellers.size() + " sellers for admin: " + adminId);
                
                // Obtener estado de conexión WebSocket de forma reactiva
                return Uni.createFrom().item(() -> {
                    Map<Long, Boolean> webSocketStatus = webSocketNotificationService.getRealTimeConnectionStatus();
                    
                    List<ConnectedSellerInfo> connectedSellers = sellers.stream()
                        .map(seller -> {
                            boolean isConnected = webSocketStatus.getOrDefault(seller.id, false);
                            LocalDateTime lastSeen = LocalDateTime.now().minusMinutes(isConnected ? 0 : 30); // Simulado
                            
                            return new ConnectedSellerInfo(
                                seller.id,
                                seller.sellerName != null ? seller.sellerName : "Vendedor " + seller.id,
                                seller.email != null ? seller.email : "email@example.com",
                                seller.phone != null ? seller.phone : "+51999999999",
                                seller.branch != null ? seller.branch.id : 0L,
                                seller.branch != null ? seller.branch.name : "Sucursal",
                                isConnected,
                                lastSeen
                            );
                        })
                        .collect(Collectors.toList());
                    
                    log.info("✅ Processed " + connectedSellers.size() + " sellers with WebSocket status");
                    return ConnectedSellersResponse.create(adminId, connectedSellers);
                });
            })
            .onFailure().recoverWithItem(throwable -> {
                log.error("❌ Error getting connected sellers: " + throwable.getMessage());
                return ConnectedSellersResponse.create(adminId, List.of());
            });
    }

    public Uni<List<SellerEntity>> getAllSellersStatusForAdmin(Long adminId) {
        return dataService.findSellersByAdminId(adminId);
    }

    public Uni<AdminPaymentManagementResponse> getConfirmedPaymentsForSellerPaginated(Long sellerId, int page, int size, LocalDate startDate, LocalDate endDate) {
        return getAllPendingPayments(page, size, startDate, endDate);
    }
    
    /**
     * Obtiene el rol del usuario por su ID
     */
    @WithTransaction
    public Uni<String> getUserRole(Long userId) {
        log.info("🔍 Getting user role for userId: " + userId);
        
        return dataService.findUserById(userId)
            .map(user -> {
                if (user == null) {
                    throw new SecurityException("Usuario no encontrado: " + userId);
                }
                String role = user.getRoleSafe().toString();
                log.info("✅ User " + userId + " has role: " + role);
                return role;
            })
            .onFailure().recoverWithItem(throwable -> {
                log.error("❌ Error getting user role: " + throwable.getMessage());
                throw new SecurityException("Error obteniendo rol del usuario: " + throwable.getMessage());
            });
    }
    
    /**
     * Obtiene pagos pendientes para un admin (todos los pagos de sus vendedores)
     */
    @WithTransaction
    public Uni<PendingPaymentsResponse> getPendingPaymentsForAdmin(Long adminId, int page, int size, LocalDate startDate, LocalDate endDate) {
        log.info("🔍 Getting pending payments for admin: " + adminId);
        
        return dataService.findPendingPaymentsForAdmin(adminId, page, size, startDate, endDate)
            .chain(payments -> {
                log.info("📊 Found " + payments.size() + " pending payments for admin: " + adminId);
                
                return dataService.countPendingPaymentsForAdmin(adminId, startDate, endDate)
                    .map(totalCount -> {
                        List<PaymentNotificationResponse> paymentResponses = payments.stream()
                            .map(PaymentNotificationMapper.ENTITY_TO_RESPONSE)
                            .collect(Collectors.toList());
                        
                        long totalPages = (long) Math.ceil((double) totalCount / size);
                        
                        return new PendingPaymentsResponse(
                            paymentResponses,
                            new PaginationInfo(
                                page,
                                (int) totalPages,
                                totalCount,
                                size,
                                page < totalPages - 1,
                                page > 0
                            )
                        );
                    });
            })
            .onFailure().recoverWithItem(throwable -> {
                log.error("❌ Error getting pending payments for admin: " + throwable.getMessage());
                return new PendingPaymentsResponse(List.of(), new PaginationInfo(page, 0, 0, size, false, false));
            });
    }
}
