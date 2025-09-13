package org.sky.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.sky.dto.payment.PaymentNotificationRequest;
import org.sky.dto.payment.PaymentClaimRequest;
import org.sky.dto.payment.PaymentNotificationResponse;
import org.sky.model.PaymentNotification;
import org.sky.model.Seller;
import org.sky.repository.PaymentNotificationRepository;
import org.sky.repository.SellerRepository;
import org.sky.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

@ApplicationScoped
public class PaymentNotificationService {
    
    @Inject
    PaymentNotificationRepository paymentNotificationRepository;
    
    @Inject
    SellerRepository sellerRepository;
    
    private static final Logger log = Logger.getLogger(PaymentNotificationService.class);
    
    // Map para almacenar las conexiones SSE activas por sellerId
    private final Map<Long, CopyOnWriteArrayList<PaymentNotificationResponse>> activeConnections = new ConcurrentHashMap<>();
    
    /**
     * Procesa una notificación de pago y la envía a todos los vendedores
     */
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
                        
                        // Broadcast a todos los vendedores del admin
                        broadcastToSellers(request.adminId(), response);
                        
                        return Uni.createFrom().item(response);
                    });
                    
        } catch (Exception e) {
            log.error("❌ Error procesando notificación de pago: " + e.getMessage());
            throw ValidationException.invalidField("paymentNotification", request.toString(), 
                "Error procesando notificación de pago: " + e.getMessage());
        }
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
     * Envía notificación a un vendedor específico
     */
    private void sendToSeller(Long sellerId, PaymentNotificationResponse notification) {
        log.info("📱 Enviando a vendedor " + sellerId + ": " + notification.message());
        
        CopyOnWriteArrayList<PaymentNotificationResponse> connections = activeConnections.get(sellerId);
        if (connections != null && !connections.isEmpty()) {
            // Enviar a todas las conexiones activas del vendedor
            connections.forEach(conn -> {
                // En un sistema real, aquí enviarías el evento SSE
                log.info("📱 SSE enviado a vendedor " + sellerId);
            });
        } else {
            log.warn("⚠️ No hay conexiones activas para vendedor " + sellerId);
        }
    }
    
    /**
     * Permite que un vendedor reclame un pago
     */
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
                                
                                // Notificar resultado a todos los vendedores
                                notifyPaymentResult(payment.adminId, response);
                                
                                return response;
                            });
                });
    }
    
    /**
     * Notifica el resultado del pago a todos los vendedores
     */
    private void notifyPaymentResult(Long adminId, PaymentNotificationResponse result) {
        log.info("📢 Notificando resultado del pago a vendedores del admin: " + adminId);
        
        sellerRepository.find("branch.admin.id = ?1 and isActive = true", adminId).list()
                .subscribe().with(sellers -> {
                    for (Seller seller : sellers) {
                        sendToSeller(seller.id, result);
                    }
                });
    }
    
    /**
     * Registra una conexión SSE para un vendedor
     */
    public void registerConnection(Long sellerId, PaymentNotificationResponse connection) {
        activeConnections.computeIfAbsent(sellerId, k -> new CopyOnWriteArrayList<>()).add(connection);
        log.info("🔗 Conexión SSE registrada para vendedor " + sellerId);
    }
    
    /**
     * Desregistra una conexión SSE para un vendedor
     */
    public void unregisterConnection(Long sellerId, PaymentNotificationResponse connection) {
        CopyOnWriteArrayList<PaymentNotificationResponse> connections = activeConnections.get(sellerId);
        if (connections != null) {
            connections.remove(connection);
            if (connections.isEmpty()) {
                activeConnections.remove(sellerId);
            }
            log.info("🔌 Conexión SSE desregistrada para vendedor " + sellerId);
        }
    }
    
    /**
     * Obtiene las conexiones activas de un vendedor
     */
    public List<PaymentNotificationResponse> getActiveConnections(Long sellerId) {
        CopyOnWriteArrayList<PaymentNotificationResponse> connections = activeConnections.get(sellerId);
        return connections != null ? List.copyOf(connections) : List.of();
    }
}
