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
import org.sky.exception.ValidationException;
import org.sky.model.PaymentNotificationEntity;
import org.sky.model.PaymentRejectionEntity;
import org.sky.model.SellerEntity;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.sky.service.websocket.WebSocketNotificationService;
import org.sky.util.DeadlockRetryService;

@ApplicationScoped
public class PaymentNotificationService {

    private static final Logger log = Logger.getLogger(PaymentNotificationService.class);

    @Inject
    PaymentNotificationDataService dataService;

    @Inject
    PaymentNotificationProcessor processor;


    @Inject
    WebSocketNotificationService webSocketNotificationService;
    
    @Inject
    DeadlockRetryService deadlockRetryService;


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
                Map<String, Object> params = new HashMap<>();
                params.put("page", page);
                params.put("size", size);
                params.put("startDate", startDate);
                params.put("endDate", endDate);
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


  @WithTransaction
    public Uni<PaymentNotificationResponse> claimPayment(Long paymentId) {
        log.info("🔍 Attempting to claim payment with ID: " + paymentId);
        
        return PaymentNotificationValidator.validatePaymentId().apply(paymentId)
            .chain(validPaymentId -> {
                log.info("✅ Payment ID validation passed: " + validPaymentId);
                
                // Usar retry automático para manejar deadlocks
                return deadlockRetryService.executeWithRetry(
                    () -> claimPaymentInternal(validPaymentId),
                    "claimPayment(" + validPaymentId + ")"
                );
            });
    }
    
    /**
     * Implementación interna del claim payment con retry automático
     */
    private Uni<PaymentNotificationResponse> claimPaymentInternal(Long paymentId) {
        return dataService.findPaymentById(paymentId)
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
            .chain(validPaymentId -> {
                // Usar retry automático para manejar deadlocks
                return deadlockRetryService.executeWithRetry(
                    () -> rejectPaymentInternal(validPaymentId, reason),
                    "rejectPayment(" + validPaymentId + ")"
                );
            });
    }
    
    /**
     * Implementación interna del reject payment con retry automático
     */
    private Uni<PaymentNotificationResponse> rejectPaymentInternal(Long paymentId, String reason) {
        return dataService.findPaymentById(paymentId)
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
      if(sellers.isEmpty()){
        return Uni.createFrom().failure(ValidationException.requiredField("sellers"));
      }

     SellerEntity selectedSeller = sellers.stream()
         .filter(seller -> seller.isActive)
         .findFirst()
         .orElse(sellers.getFirst());



      return createNotificationForSeller(request,selectedSeller);
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

  public Uni<List<SellerEntity>> getAllSellersStatusForAdmin(Long adminId) {
        return dataService.findSellersByAdminId(adminId);
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
     * Obtiene pagos para un admin por estado específico (todos los pagos de sus vendedores)
     */
    @WithTransaction
    public Uni<AdminPaymentManagementResponse> getPaymentsForAdminByStatus(Long adminId, int page, int size, String status, LocalDate startDate, LocalDate endDate) {
        log.info("🔍 Getting payments for admin: " + adminId + " with status: " + status);
        
        return dataService.findPaymentsForAdminByStatus(adminId, page, size, status, startDate, endDate)
            .chain(payments -> {
                log.info("📊 Found " + payments.size() + " payments for admin: " + adminId + " with status: " + status);
                
                return dataService.countPaymentsForAdminByStatus(adminId, status, startDate, endDate)
                    .chain(totalCount -> calculatePaymentSummary(adminId, status, startDate, endDate)
                        .map(summary -> {
                            List<PaymentDetail> paymentDetails = mapPaymentsToDetails(payments);
                            PaginationInfo pagination = PaginationInfo.create(page, totalCount, size);
                            
                            return new AdminPaymentManagementResponse(paymentDetails, summary, pagination);
                        })
                    );
            })
            .onFailure().invoke(throwable -> {
                log.error("❌ Error getting payments for admin: " + throwable.getMessage());
            });
    }
    
    /**
     * Calcula el PaymentSummary completo independiente de la paginación
     * Este método obtiene TODOS los datos para el resumen, no solo los de la página actual
     */
    private Uni<PaymentSummary> calculatePaymentSummary(Long adminId, String status, LocalDate startDate, LocalDate endDate) {
        log.info("🧮 Calculating payment summary for admin: " + adminId + " with status: " + status);
        
        return getPaymentCounts(adminId, startDate, endDate)
            .chain(counts -> getPaymentAmounts(adminId, startDate, endDate)
                .map(amounts -> createConsistentSummary(status, counts, amounts))
            );
    }

  /**
     * Obtiene conteos de pagos por estado de forma reactiva
     */
    private Uni<PaymentCounts> getPaymentCounts(Long adminId, LocalDate startDate, LocalDate endDate) {
        return dataService.countPaymentsForAdminByStatus(adminId, "PENDING", startDate, endDate)
            .chain(pendingCount -> 
                dataService.countPaymentsForAdminByStatus(adminId, "CLAIMED", startDate, endDate)
                    .chain(confirmedCount ->
                        dataService.countPaymentsForAdminByStatus(adminId, "REJECTED", startDate, endDate)
                            .map(rejectedCount -> new PaymentCounts(pendingCount, confirmedCount, rejectedCount))
                    )
            );
    }
    
    /**
     * Obtiene montos de pagos por estado de forma reactiva
     */
    private Uni<PaymentAmounts> getPaymentAmounts(Long adminId, LocalDate startDate, LocalDate endDate) {
        return dataService.sumAmountForAdminByStatus(adminId, "PENDING", startDate, endDate)
            .chain(pendingAmount -> 
                dataService.sumAmountForAdminByStatus(adminId, "CLAIMED", startDate, endDate)
                    .chain(confirmedAmount ->
                        dataService.sumAmountForAdminByStatus(adminId, "REJECTED", startDate, endDate)
                            .map(rejectedAmount -> new PaymentAmounts(pendingAmount, confirmedAmount, rejectedAmount))
                    )
            );
    }
    
    /**
     * Crea un PaymentSummary consistente basado en el filtro aplicado
     */
    private PaymentSummary createConsistentSummary(String status, PaymentCounts counts, PaymentAmounts amounts) {
        if ("PENDING".equalsIgnoreCase(status)) {
            return new PaymentSummary(
                counts.pendingCount(),
                counts.pendingCount(),
                0L,
                0L,
                amounts.pendingAmount(),
                0.0,
                amounts.pendingAmount()
            );
        } else if ("CLAIMED".equalsIgnoreCase(status)) {
            return new PaymentSummary(
                counts.confirmedCount(),
                0L,
                counts.confirmedCount(),
                0L,
                amounts.confirmedAmount(),
                amounts.confirmedAmount(),
                0.0
            );
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            return new PaymentSummary(
                counts.rejectedCount(),
                0L,
                0L,
                counts.rejectedCount(),
                amounts.rejectedAmount(),
                0.0,
                0.0
            );
        } else {
            // Para múltiples estados o ALL, usar todos los datos
            long totalPayments = counts.pendingCount() + counts.confirmedCount() + counts.rejectedCount();
            double totalAmount = amounts.pendingAmount() + amounts.confirmedAmount() + amounts.rejectedAmount();
            
            return new PaymentSummary(
                totalPayments,
                counts.pendingCount(),
                counts.confirmedCount(),
                counts.rejectedCount(),
                totalAmount,
                amounts.confirmedAmount(),
                amounts.pendingAmount()
            );
        }
    }
    
    /**
     * Mapea las entidades de pago a PaymentDetail
     */
    private List<PaymentDetail> mapPaymentsToDetails(List<PaymentNotificationEntity> payments) {
        return payments.stream()
            .map(payment -> new PaymentDetail(
                payment.id,
                payment.amount,
                payment.senderName,
                payment.yapeCode,
                payment.status,
                payment.createdAt,
                payment.confirmedBy,
                payment.confirmedAt,
                payment.rejectedBy,
                payment.rejectedAt,
                payment.rejectionReason,
                "Seller Name", // sellerName - would need to be fetched
                "Branch Name"  // branchName - would need to be fetched
            ))
            .collect(Collectors.toList());
    }
    
    /**
     * Record para encapsular conteos de pagos
     */
    private record PaymentCounts(long pendingCount, long confirmedCount, long rejectedCount) {}
    
    /**
     * Record para encapsular montos de pagos
     */
    private record PaymentAmounts(double pendingAmount, double confirmedAmount, double rejectedAmount) {}

}
