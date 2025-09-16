package org.sky.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.sky.dto.payment.PaymentClaimRequest;
import org.sky.dto.payment.PaymentNotificationRequest;
import org.sky.dto.payment.PaymentNotificationResponse;
import org.sky.dto.payment.PaymentRejectRequest;
import org.sky.dto.payment.PendingPaymentsResponse;
import org.sky.dto.payment.AdminPaymentManagementResponse;
import org.sky.model.PaymentNotification;
import org.sky.model.Seller;
import org.sky.repository.PaymentNotificationRepository;
import org.sky.repository.SellerRepository;
import org.sky.exception.ValidationException;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.vertx.core.Vertx;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class PaymentNotificationService {
    
    @Inject
    PaymentNotificationRepository paymentNotificationRepository;
    
    @Inject
    SellerRepository sellerRepository;
    
    @Inject
    WebSocketNotificationService webSocketNotificationService;
    
    @Inject
    Vertx vertx;
    
    private static final Logger log = Logger.getLogger(PaymentNotificationService.class);
    
    
    /**
     * Procesa una notificación de pago y la envía a todos los vendedores
     */
    @WithTransaction
    public Uni<PaymentNotificationResponse> processPaymentNotification(PaymentNotificationRequest request) {
        log.info("💰 PaymentNotificationService.processPaymentNotification() - Procesando nuevo pago");
        log.info("💰 AdminId: " + request.adminId());
        log.info("💰 Amount: " + request.amount());
        log.info("💰 SenderName: " + request.senderName());
        log.info("💰 YapeCode: " + request.yapeCode());

        // Crear nueva notificación de pago
        PaymentNotification payment = new PaymentNotification();
        payment.adminId = request.adminId();
        payment.amount = request.amount();
        payment.senderName = request.senderName();
        payment.yapeCode = request.yapeCode();
        payment.status = "PENDING";
        payment.createdAt = LocalDateTime.now();

        return paymentNotificationRepository.persist(payment)
                .chain(savedPayment -> {
                    log.info("✅ Pago guardado en BD con ID: " + savedPayment.id);

                    // Crear respuesta
                    PaymentNotificationResponse response = new PaymentNotificationResponse(
                        savedPayment.id,
                        savedPayment.amount,
                        savedPayment.senderName,
                        savedPayment.yapeCode,
                        savedPayment.status,
                        savedPayment.createdAt,
                        "Pago pendiente de confirmación"
                    );
                    
                    // Enviar notificación WebSocket a todos los vendedores del admin
                    return broadcastToSellersReactive(request.adminId(), response)
                            .map(v -> response);
                });
    }
    
    /**
     * Envía notificación a todos los vendedores de un admin via WebSocket (reactivo)
     */
    public Uni<Void> broadcastToSellersReactive(Long adminId, PaymentNotificationResponse notification) {
        log.info("📡 PaymentNotificationService.broadcastToSellersReactive() - AdminId: " + adminId);

        return sellerRepository.find("branch.admin.id = ?1", adminId)
                .list()
                .map(sellers -> {
                    log.info("📡 Encontrados " + sellers.size() + " vendedores para admin " + adminId);

                    for (Seller seller : sellers) {
                        log.info("📡 Enviando a vendedor " + seller.id + " (" + seller.sellerName + ")");
                        sendToSeller(seller.id, notification);
                    }
                    return null; // Retorna null para Uni<Void>
                })
                .replaceWithVoid()
                .onFailure().invoke(error -> {
                    log.error("❌ Error obteniendo vendedores para admin " + adminId + ": " + error.getMessage());
                });
    }
    
    /**
     * Envía notificación a un vendedor específico via WebSocket
     */
    private void sendToSeller(Long sellerId, PaymentNotificationResponse notification) {
        log.info("📱 Enviando a vendedor " + sellerId + ": " + notification.message());
        
        // Convertir notificación a JSON
        String notificationJson = convertToJson(notification);
        
        // Enviar via WebSocket usando el servicio
        webSocketNotificationService.sendNotification(sellerId, notificationJson);
    }
    
    /**
     * Convierte PaymentNotificationResponse a JSON
     */
    private String convertToJson(PaymentNotificationResponse notification) {
        return String.format(
            "{\"type\":\"PAYMENT_NOTIFICATION\",\"data\":{\"paymentId\":%d,\"amount\":%.2f,\"senderName\":\"%s\",\"yapeCode\":\"%s\",\"status\":\"%s\",\"timestamp\":\"%s\",\"message\":\"%s\"}}",
            notification.paymentId(),
            notification.amount(),
            notification.senderName(),
            notification.yapeCode(),
            notification.status(),
            notification.timestamp(),
            notification.message()
        );
    }
    
    /**
     * Envía notificación directamente a un vendedor (para pruebas)
     */
    public void sendToSellerDirectly(Long sellerId, PaymentNotificationResponse notification) {
        log.info("🧪 Enviando notificación directa a vendedor " + sellerId);
        sendToSeller(sellerId, notification);
    }
    
    /**
     * Permite que un vendedor reclame un pago
     */
    @WithTransaction
    public Uni<PaymentNotificationResponse> claimPayment(PaymentClaimRequest request) {
        log.info("🎯 PaymentNotificationService.claimPayment() - Vendedor reclamando pago");
        log.info("🎯 SellerId: " + request.sellerId());
        log.info("🎯 PaymentId: " + request.paymentId());
        
        return paymentNotificationRepository.findById(request.paymentId())
                .chain(payment -> {
                    if (payment == null) {
                        return Uni.createFrom().failure(
                            ValidationException.invalidField("paymentId", request.paymentId().toString(), 
                                "Pago no encontrado")
                        );
                    }
                    
                    if (!"PENDING".equals(payment.status)) {
                        return Uni.createFrom().failure(
                            ValidationException.invalidField("paymentId", request.paymentId().toString(), 
                                "El pago ya fue procesado")
                        );
                    }
                    
                    // Marcar como confirmado
                    payment.status = "CONFIRMED";
                    payment.confirmedBy = request.sellerId();
                    payment.confirmedAt = LocalDateTime.now();
                    
                    return paymentNotificationRepository.persist(payment)
                            .map(confirmedPayment -> {
                                log.info("✅ Pago confirmado por vendedor " + request.sellerId());
                                
                                // Crear respuesta
                                PaymentNotificationResponse response = new PaymentNotificationResponse(
                                    confirmedPayment.id,
                                    confirmedPayment.amount,
                                    confirmedPayment.senderName,
                                    confirmedPayment.yapeCode,
                                    confirmedPayment.status,
                                    confirmedPayment.confirmedAt,
                                    "Pago confirmado por vendedor " + request.sellerId()
                                );
                                
                                // Notificar resultado a todos los vendedores (reactivamente)
                                notifyPaymentResultReactive(payment.adminId, response);
                                
                                return response;
                            });
                });
    }
    
    /**
     * Permite que un vendedor rechace un pago
     */
    @WithTransaction
    public Uni<PaymentNotificationResponse> rejectPayment(PaymentRejectRequest request) {
        log.info("❌ PaymentNotificationService.rejectPayment() - Vendedor rechazando pago");
        log.info("❌ SellerId: " + request.sellerId());
        log.info("❌ PaymentId: " + request.paymentId());
        log.info("❌ Reason: " + request.reason());
        
        return paymentNotificationRepository.findById(request.paymentId())
                .chain(payment -> {
                    if (payment == null) {
                        return Uni.createFrom().failure(
                            ValidationException.invalidField("paymentId", request.paymentId().toString(), 
                                "Pago no encontrado")
                        );
                    }
                    
                    if (!"PENDING".equals(payment.status)) {
                        return Uni.createFrom().failure(
                            ValidationException.invalidField("paymentId", request.paymentId().toString(), 
                                "El pago ya fue procesado")
                        );
                    }
                    
                    // Marcar como rechazado por este vendedor
                    payment.status = "REJECTED_BY_SELLER";
                    payment.rejectedBy = request.sellerId();
                    payment.rejectedAt = LocalDateTime.now();
                    payment.rejectionReason = request.reason() != null ? request.reason() : "Sin razón especificada";
                    
                    return paymentNotificationRepository.persist(payment)
                            .map(rejectedPayment -> {
                                log.info("❌ Pago rechazado por vendedor " + request.sellerId() + " - Razón: " + request.reason());
                                
                                // Crear respuesta
                                PaymentNotificationResponse response = new PaymentNotificationResponse(
                                    rejectedPayment.id,
                                    rejectedPayment.amount,
                                    rejectedPayment.senderName,
                                    rejectedPayment.yapeCode,
                                    rejectedPayment.status,
                                    rejectedPayment.rejectedAt,
                                    "Pago rechazado por vendedor " + request.sellerId() + " - " + request.reason()
                                );
                                
                                // Notificar resultado a todos los vendedores (reactivamente)
                                notifyPaymentResultReactive(payment.adminId, response);
                                
                                return response;
                            });
                });
    }
    
    /**
     * Notifica el resultado del pago a todos los vendedores (reactivamente)
     */
    private void notifyPaymentResultReactive(Long adminId, PaymentNotificationResponse result) {
        log.info("📢 Notificando resultado del pago a vendedores del admin: " + adminId);
        
        sellerRepository.find("branch.admin.id = ?1 and isActive = true", adminId).list()
                .subscribe().with(sellers -> {
                    log.info("📡 Encontrados " + sellers.size() + " vendedores para notificar");
                    for (Seller seller : sellers) {
                        sendToSeller(seller.id, result);
                    }
                }, failure -> {
                    log.error("❌ Error al obtener vendedores para notificar: " + failure.getMessage());
                });
    }
    
    
    /**
     * Obtiene todos los pagos pendientes para un vendedor específico
     */
    @WithTransaction
    public Uni<List<PaymentNotificationResponse>> getPendingPaymentsForSeller(Long sellerId) {
        log.info("📋 PaymentNotificationService.getPendingPaymentsForSeller() - Obteniendo pagos pendientes para vendedor: " + sellerId);
        
        return paymentNotificationRepository.find("status = ?1", "PENDING").list()
                .map(payments -> {
                    log.info("📋 Encontrados " + payments.size() + " pagos pendientes");
                    
                    return payments.stream()
                            .map(payment -> new PaymentNotificationResponse(
                                payment.id,
                                payment.amount,
                                payment.senderName,
                                payment.yapeCode,
                                payment.status,
                                payment.createdAt,
                                "Pago pendiente de confirmación"
                            ))
                            .collect(java.util.stream.Collectors.toList());
                });
    }
    
    /**
     * Obtiene los pagos pendientes para un vendedor específico con paginación
     */
    @WithTransaction
    public Uni<PendingPaymentsResponse> getPendingPaymentsForSellerPaginated(Long sellerId, int page, int size) {
        log.info("📋 PaymentNotificationService.getPendingPaymentsForSellerPaginated() - Obteniendo pagos pendientes paginados para vendedor: " + sellerId);
        log.info("📋 Página: " + page + ", Tamaño: " + size);
        
        // Validar parámetros de paginación
        final int validatedPage = Math.max(0, page);
        final int validatedSize = (size <= 0 || size > 100) ? 20 : size;
        
        // Obtener el total de pagos pendientes
        return paymentNotificationRepository.count("status = ?1", "PENDING")
                .chain(totalCount -> {
                    log.info("📋 Total de pagos pendientes: " + totalCount);
                    
                    // Calcular información de paginación
                    int totalPages = (int) Math.ceil((double) totalCount / validatedSize);
                    int currentPage = Math.min(validatedPage, Math.max(0, totalPages - 1));
                    
                    // Obtener los pagos paginados
                    return paymentNotificationRepository.find("status = ?1 order by createdAt desc", "PENDING")
                            .page(currentPage, validatedSize)
                            .list()
                            .map(payments -> {
                                log.info("📋 Encontrados " + payments.size() + " pagos en página " + currentPage);
                                
                                List<PaymentNotificationResponse> paymentResponses = payments.stream()
                                        .map(payment -> new PaymentNotificationResponse(
                                            payment.id,
                                            payment.amount,
                                            payment.senderName,
                                            payment.yapeCode,
                                            payment.status,
                                            payment.createdAt,
                                            "Pago pendiente de confirmación"
                                        ))
                                        .collect(java.util.stream.Collectors.toList());
                                
                                PendingPaymentsResponse.PaginationInfo paginationInfo = 
                                    new PendingPaymentsResponse.PaginationInfo(
                                        currentPage,
                                        totalPages,
                                        totalCount,
                                        validatedSize
                                    );
                                
                                return new PendingPaymentsResponse(paymentResponses, paginationInfo);
                            });
                });
    }
    
    /**
     * Obtiene todos los pagos pendientes con paginación (para ADMINs)
     */
    @WithTransaction
    public Uni<PendingPaymentsResponse> getAllPendingPaymentsPaginated(int page, int size) {
        log.info("📋 PaymentNotificationService.getAllPendingPaymentsPaginated() - Obteniendo todos los pagos pendientes");
        log.info("📋 Página: " + page + ", Tamaño: " + size);

        // Validar parámetros de paginación
        final int validatedPage = Math.max(0, page);
        final int validatedSize = (size <= 0 || size > 100) ? 20 : size;

        // Obtener el total de pagos pendientes
        return paymentNotificationRepository.count("status = ?1", "PENDING")
                .chain(totalCount -> {
                    log.info("📋 Total de pagos pendientes: " + totalCount);

                    // Calcular información de paginación
                    int totalPages = (int) Math.ceil((double) totalCount / validatedSize);
                    int currentPage = Math.min(validatedPage, Math.max(0, totalPages - 1));

                    // Obtener los pagos paginados
                    return paymentNotificationRepository.find("status = ?1 order by createdAt desc", "PENDING")
                            .page(currentPage, validatedSize)
                            .list()
                            .map(payments -> {
                                log.info("📋 Encontrados " + payments.size() + " pagos en página " + currentPage);

                                List<PaymentNotificationResponse> paymentResponses = payments.stream()
                                        .map(payment -> new PaymentNotificationResponse(
                                            payment.id,
                                            payment.amount,
                                            payment.senderName,
                                            payment.yapeCode,
                                            payment.status,
                                            payment.createdAt,
                                            "Pago pendiente de confirmación"
                                        ))
                                        .collect(java.util.stream.Collectors.toList());

                                PendingPaymentsResponse.PaginationInfo paginationInfo =
                                    new PendingPaymentsResponse.PaginationInfo(
                                        currentPage,
                                        totalPages,
                                        totalCount,
                                        validatedSize
                                    );

                                return new PendingPaymentsResponse(paymentResponses, paginationInfo);
                            });
                });
    }
    
    /**
     * Obtiene todos los pagos para gestión de administrador con información detallada
     */
    @WithTransaction
    public Uni<AdminPaymentManagementResponse> getAdminPaymentManagement(Long adminId, int page, int size, String status) {
        log.info("👑 PaymentNotificationService.getAdminPaymentManagement() - AdminId: " + adminId + 
                ", Página: " + page + ", Tamaño: " + size + ", Status: " + status);
        
        // Construir query basada en filtros
        String query = "adminId = ?1";
        List<Object> params = new ArrayList<>();
        params.add(adminId);
        
        if (status != null && !status.isEmpty()) {
            query += " and status = ?2";
            params.add(status);
        }
        
        return paymentNotificationRepository.find(query, params.toArray())
                .page(page, size)
                .list()
                .chain(payments -> {
                    // Obtener información de vendedores
                    return sellerRepository.find("branch.admin.id = ?1", adminId)
                            .list()
                            .map(sellers -> {
                                Map<Long, Seller> sellerMap = sellers.stream()
                                        .collect(Collectors.toMap(seller -> seller.id, seller -> seller));
                                
                                // Convertir pagos a detalles con información de vendedor
                                List<AdminPaymentManagementResponse.PaymentDetail> paymentDetails = 
                                    payments.stream()
                                        .map(payment -> {
                                            Seller seller = sellerMap.get(payment.confirmedBy);
                                            String sellerName = seller != null ? seller.sellerName : "Sin asignar";
                                            String branchName = seller != null && seller.branch != null ? 
                                                seller.branch.name : "Sin sucursal";
                                            
                                            return new AdminPaymentManagementResponse.PaymentDetail(
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
                                                sellerName,
                                                branchName
                                            );
                                        })
                                        .collect(Collectors.toList());
                                
                                // Calcular resumen
                                AdminPaymentManagementResponse.PaymentSummary summary = calculatePaymentSummary(payments);
                                
                                // Información de paginación (simplificada)
                                AdminPaymentManagementResponse.PaginationInfo pagination = 
                                    new AdminPaymentManagementResponse.PaginationInfo(
                                        page,
                                        1, // totalPages simplificado
                                        payments.size(),
                                        size
                                    );
                                
                                return new AdminPaymentManagementResponse(paymentDetails, summary, pagination);
                            });
                });
    }
    
    private AdminPaymentManagementResponse.PaymentSummary calculatePaymentSummary(List<PaymentNotification> payments) {
        long totalPayments = payments.size();
        long pendingCount = payments.stream().filter(p -> "PENDING".equals(p.status)).count();
        long confirmedCount = payments.stream().filter(p -> "CONFIRMED".equals(p.status)).count();
        long rejectedCount = payments.stream().filter(p -> "REJECTED_BY_SELLER".equals(p.status)).count();
        
        double totalAmount = payments.stream().mapToDouble(p -> p.amount).sum();
        double confirmedAmount = payments.stream()
                .filter(p -> "CONFIRMED".equals(p.status))
                .mapToDouble(p -> p.amount)
                .sum();
        double pendingAmount = payments.stream()
                .filter(p -> "PENDING".equals(p.status))
                .mapToDouble(p -> p.amount)
                .sum();
        
        return new AdminPaymentManagementResponse.PaymentSummary(
            totalPayments, pendingCount, confirmedCount, rejectedCount,
            totalAmount, confirmedAmount, pendingAmount
        );
    }
    
}
