package org.sky.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.sky.dto.payment.PaymentNotificationRequest;
import org.sky.dto.payment.PaymentClaimRequest;
import org.sky.dto.payment.PaymentNotificationResponse;
import org.sky.dto.payment.PendingPaymentsResponse;
import org.sky.model.PaymentNotification;
import org.sky.model.Seller;
import org.sky.repository.PaymentNotificationRepository;
import org.sky.repository.SellerRepository;
import org.sky.exception.ValidationException;
import org.sky.service.WebSocketNotificationService;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.vertx.core.Vertx;

import java.time.LocalDateTime;
import java.util.List;

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
        log.info("💰 PaymentNotificationService.processPaymentNotification() - Procesando pago");
        log.info("💰 AdminId: " + request.adminId());
        log.info("💰 Monto: " + request.amount());
        log.info("💰 Remitente: " + request.senderName());
        log.info("💰 Código: " + request.yapeCode());
        
        try {
            // Crear notificación de pago
            PaymentNotification paymentNotification = new PaymentNotification();
            paymentNotification.adminId = request.adminId();
            paymentNotification.amount = request.amount();
            paymentNotification.senderName = request.senderName();
            paymentNotification.yapeCode = request.yapeCode();
            paymentNotification.status = "PENDING";
            paymentNotification.createdAt = LocalDateTime.now();
            
            // Guardar en base de datos
            return paymentNotificationRepository.persist(paymentNotification)
                    .chain(savedNotification -> {
                        log.info("💾 Notificación de pago guardada con ID: " + savedNotification.id);
                        
                        // Crear respuesta
                        PaymentNotificationResponse response = new PaymentNotificationResponse(
                            savedNotification.id,
                            savedNotification.amount,
                            savedNotification.senderName,
                            savedNotification.yapeCode,
                            savedNotification.status,
                            savedNotification.createdAt,
                            "Pago pendiente de confirmación"
                        );
                        
                        // Broadcast a todos los vendedores del admin (reactivamente)
                        return broadcastToSellersReactive(request.adminId(), response)
                                .map(v -> response);
                    });
                    
        } catch (Exception e) {
            log.error("❌ Error procesando notificación de pago: " + e.getMessage());
            throw ValidationException.invalidField("paymentNotification", request.toString(), 
                "Error procesando notificación de pago: " + e.getMessage());
        }
    }
    
    /**
     * Broadcast de notificación a todos los vendedores de un admin (reactivamente)
     */
    private Uni<Void> broadcastToSellersReactive(Long adminId, PaymentNotificationResponse notification) {
        log.info("📡 Broadcast a vendedores del admin: " + adminId);
        
        return sellerRepository.find("branch.admin.id = ?1 and isActive = true", adminId).list()
                .map(sellers -> {
                    log.info("📡 Enviando a " + sellers.size() + " vendedores");
                    
                    for (Seller seller : sellers) {
                        // Enviar a cada vendedor
                        sendToSeller(seller.id, notification);
                    }
                    return null; // Retornar Void
                });
    }
    
    /**
     * Broadcast de notificación a todos los vendedores de un admin
     */
    private void broadcastToSellers(Long adminId, PaymentNotificationResponse notification) {
        log.info("📡 Broadcast a vendedores del admin: " + adminId);
        
        // Obtener todos los vendedores activos del admin
        sellerRepository.find("branch.admin.id = ?1 and isActive = true", adminId).list()
                .subscribe().with(sellers -> {
                    log.info("📡 Enviando a " + sellers.size() + " vendedores");
                    
                    for (Seller seller : sellers) {
                        // Enviar a cada vendedor
                        sendToSeller(seller.id, notification);
                    }
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
     * Notifica el resultado del pago a todos los vendedores (fuera de transacción)
     */
    private void notifyPaymentResult(Long adminId, PaymentNotificationResponse result) {
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
    
}
