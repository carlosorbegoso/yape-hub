package org.sky.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.sky.dto.payment.PaymentClaimRequest;
import org.sky.dto.payment.PaymentClaimResponse;
import org.sky.dto.payment.PaymentNotificationRequest;
import org.sky.dto.payment.PaymentNotificationResponse;
import org.sky.dto.payment.PaymentRejectRequest;
import org.sky.dto.payment.PaymentRejectResponse;
import org.sky.dto.payment.PendingPaymentsResponse;
import org.sky.dto.payment.AdminPaymentManagementResponse;
import org.sky.model.PaymentNotification;
import org.sky.model.PaymentRejection;
import org.sky.model.Seller;
import org.sky.repository.PaymentNotificationRepository;
import org.sky.repository.PaymentRejectionRepository;
import org.sky.repository.SellerRepository;
import org.sky.exception.ValidationException;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.vertx.core.Vertx;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@ApplicationScoped
public class PaymentNotificationService {
    
    @Inject
    PaymentNotificationRepository paymentNotificationRepository;
    
    @Inject
    PaymentRejectionRepository paymentRejectionRepository;
    
    @Inject
    SellerRepository sellerRepository;
    
    @Inject
    org.sky.service.websocket.WebSocketNotificationService webSocketNotificationService;
    
    @Inject
    Vertx vertx;
    
    private static final Logger log = Logger.getLogger(PaymentNotificationService.class);
    
    // Cache para agrupación de notificaciones múltiples
    private final Map<Long, List<PaymentNotificationResponse>> notificationQueue = new ConcurrentHashMap<>();
    private final Map<Long, AtomicInteger> notificationCounters = new ConcurrentHashMap<>();
    private final Map<Long, Long> lastNotificationTime = new ConcurrentHashMap<>();
    private final Map<Long, Long> timerIds = new ConcurrentHashMap<>();
    
    // Configuración de agrupación
    private static final long GROUPING_WINDOW_MS = 5000; // 5 segundos
    private static final int MAX_NOTIFICATIONS_PER_GROUP = 5;
    
    
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

        // Verificar duplicados usando el deduplicationHash del frontend
        log.info("🔍 Verificando duplicados para hash: " + request.deduplicationHash());
        
        return paymentNotificationRepository.find("deduplicationHash = ?1", request.deduplicationHash())
                .firstResult()
                .chain(existingPayment -> {
                    if (existingPayment != null) {
                        log.warn("⚠️ Transacción duplicada detectada para hash: " + request.deduplicationHash());
                        log.warn("⚠️ ID de transacción existente: " + existingPayment.id);
                        
                        // Crear respuesta con la transacción existente
                        PaymentNotificationResponse response = new PaymentNotificationResponse(
                            existingPayment.id,
                            existingPayment.amount,
                            existingPayment.senderName,
                            existingPayment.yapeCode,
                            existingPayment.status,
                            existingPayment.createdAt,
                            "Transacción ya procesada anteriormente"
                        );
                        
                        return Uni.createFrom().item(response);
                    }
                    
                    // Crear nueva notificación de pago solo si no existe
                    PaymentNotification payment = new PaymentNotification();
                    payment.adminId = request.adminId();
                    payment.amount = request.amount();
                    payment.senderName = request.senderName();
                    payment.yapeCode = request.yapeCode();
                    payment.deduplicationHash = request.deduplicationHash();
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
                                
                                // Enviar notificación WebSocket a todos los vendedores del admin (reactivo)
                                return broadcastToSellersReactive(request.adminId(), response)
                                        .replaceWith(response);
                            });
                });
    }
    
    /**
     * Envía notificación a todos los vendedores de un admin via WebSocket (reactivo)
     */
    private Uni<Void> broadcastToSellersReactive(Long adminId, PaymentNotificationResponse notification) {
        log.info("📡 PaymentNotificationService.broadcastToSellersReactive() - AdminId: " + adminId);

        return sellerRepository.find("branch.admin.id = ?1", adminId)
                .list()
                .map(sellers -> {
                    log.info("📡 Encontrados " + sellers.size() + " vendedores para admin " + adminId);

                    for (Seller seller : sellers) {
                        log.info("📡 Enviando a vendedor " + seller.id + " (" + seller.sellerName + ")");
                        sendToSeller(seller.id, notification);
                    }
                    return null;
                })
                .replaceWithVoid()
                .onFailure().invoke(failure -> {
                    log.error("❌ Error obteniendo vendedores para admin " + adminId + ": " + failure.getMessage());
                });
    }
    
    /**
     * Envía notificación a un vendedor específico via WebSocket con agrupación inteligente
     */
    private void sendToSeller(Long sellerId, PaymentNotificationResponse notification) {
        log.info("📱 Enviando a vendedor " + sellerId + ": " + notification.message());
        
        // Agregar a cola de notificaciones para agrupación
        addToNotificationQueue(sellerId, notification);
        
        // Programar envío con debounce
        scheduleNotificationDelivery(sellerId);
    }
    
    /**
     * Agrega notificación a la cola de agrupación
     */
    private void addToNotificationQueue(Long sellerId, PaymentNotificationResponse notification) {
        notificationQueue.computeIfAbsent(sellerId, k -> new ArrayList<>()).add(notification);
        notificationCounters.computeIfAbsent(sellerId, k -> new AtomicInteger(0)).incrementAndGet();
        lastNotificationTime.put(sellerId, System.currentTimeMillis());
        
        log.info("📦 Notificación agregada a cola para vendedor " + sellerId + 
                " (Total en cola: " + notificationQueue.get(sellerId).size() + ")");
    }
    
    /**
     * Programa el envío de notificaciones con debounce
     */
    private void scheduleNotificationDelivery(Long sellerId) {
        // Cancelar timer anterior si existe
        Long existingTimerId = getTimerId(sellerId);
        if (existingTimerId != null) {
            vertx.cancelTimer(existingTimerId);
        }
        
        // Programar nuevo envío
        long timerId = vertx.setTimer(GROUPING_WINDOW_MS, id -> {
            processNotificationQueue(sellerId);
        });
        
        setTimerId(sellerId, timerId);
    }
    
    /**
     * Procesa la cola de notificaciones para un vendedor
     */
    private void processNotificationQueue(Long sellerId) {
        List<PaymentNotificationResponse> notifications = notificationQueue.get(sellerId);
        if (notifications == null || notifications.isEmpty()) {
            return;
        }
        
        // Limpiar cola
        notificationQueue.remove(sellerId);
        notificationCounters.remove(sellerId);
        lastNotificationTime.remove(sellerId);
        
        if (notifications.size() == 1) {
            // Envío individual
            sendIndividualNotification(sellerId, notifications.get(0));
        } else {
            // Envío agrupado
            sendGroupedNotification(sellerId, notifications);
        }
    }
    
    /**
     * Envía notificación individual
     */
    private void sendIndividualNotification(Long sellerId, PaymentNotificationResponse notification) {
        log.info("📱 Enviando notificación individual a vendedor " + sellerId);
        
        String notificationJson = convertToJson(notification);
        webSocketNotificationService.sendNotification(sellerId, notificationJson);
    }
    
    /**
     * Envía notificación agrupada
     */
    private void sendGroupedNotification(Long sellerId, List<PaymentNotificationResponse> notifications) {
        log.info("📦 Enviando notificación agrupada a vendedor " + sellerId + 
                " (" + notifications.size() + " pagos)");
        
        // Calcular total
        double totalAmount = notifications.stream().mapToDouble(PaymentNotificationResponse::amount).sum();
        int count = notifications.size();
        
        // Crear notificación agrupada
        String groupedJson = String.format(
            "{\"type\":\"GROUPED_PAYMENT_NOTIFICATION\",\"data\":{\"count\":%d,\"totalAmount\":%.2f,\"payments\":[%s],\"message\":\"%d nuevos pagos recibidos - Total: S/ %.2f\"}}",
            count,
            totalAmount,
            notifications.stream()
                .map(n -> String.format("{\"paymentId\":%d,\"amount\":%.2f,\"senderName\":\"%s\",\"yapeCode\":\"%s\"}", 
                    n.paymentId(), n.amount(), n.senderName(), n.yapeCode()))
                .collect(Collectors.joining(",")),
            count,
            totalAmount
        );
        
        webSocketNotificationService.sendNotification(sellerId, groupedJson);
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
    public Uni<PaymentClaimResponse> claimPayment(PaymentClaimRequest request) {
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
                                
                                // Crear respuesta específica para claim
                                PaymentClaimResponse response = new PaymentClaimResponse(
                                    confirmedPayment.id,
                                    confirmedPayment.amount,
                                    confirmedPayment.senderName,
                                    confirmedPayment.yapeCode,
                                    confirmedPayment.status,
                                    confirmedPayment.confirmedAt,
                                    "Pago confirmado por vendedor " + request.sellerId(),
                                    confirmedPayment.confirmedBy,
                                    confirmedPayment.confirmedAt
                                );
                                
                                return response;
                            })
                                .chain(response -> {
                                    // Notificar resultado a todos los vendedores
                                    return notifyPaymentClaimResultSimple(payment.adminId, response.paymentId(), response.amount(), response.senderName(), response.yapeCode(), response.status(), response.message())
                                            .map(v -> response);
                                });
                });
    }
    
    /**
     * Permite que un vendedor rechace un pago
     */
    @WithTransaction
    public Uni<PaymentRejectResponse> rejectPayment(PaymentRejectRequest request) {
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
                    
                    // Verificar si este vendedor ya rechazó este pago
                    return paymentRejectionRepository.find("paymentNotificationId = ?1 and sellerId = ?2", 
                            request.paymentId(), request.sellerId())
                            .firstResult()
                            .chain(existingRejection -> {
                                if (existingRejection != null) {
                                    return Uni.createFrom().failure(
                                        ValidationException.invalidField("paymentId", request.paymentId().toString(), 
                                            "Ya rechazaste este pago anteriormente")
                                    );
                                }
                                
                                // Crear nuevo rechazo
                                PaymentRejection rejection = new PaymentRejection(
                                    request.paymentId(),
                                    request.sellerId(),
                                    request.reason() != null ? request.reason() : "Sin razón especificada"
                                );
                                
                                return paymentRejectionRepository.persist(rejection)
                                        .chain(savedRejection -> {
                                            // Contar cuántos vendedores han rechazado este pago
                                            return paymentRejectionRepository.count("paymentNotificationId = ?1", request.paymentId())
                                                    .chain(rejectionCount -> {
                                                        // Contar cuántos vendedores hay en total para este admin
                                                        return sellerRepository.count("branch.admin.id = ?1 and isActive = true", payment.adminId)
                                                                .map(totalSellers -> {
                                                                    log.info("❌ Rechazo registrado. Rechazos: " + rejectionCount + ", Total vendedores: " + totalSellers);
                                                                    
                                                                    // Si todos los vendedores han rechazado, marcar como completamente rechazado
                                                                    if (rejectionCount >= totalSellers) {
                                                                        payment.status = "REJECTED_BY_ALL_SELLERS";
                                                                        payment.rejectedBy = request.sellerId();
                                                                        payment.rejectedAt = LocalDateTime.now();
                                                                        payment.rejectionReason = "Rechazado por todos los vendedores";
                                                                        log.info("❌ Pago marcado como rechazado por todos los vendedores");
                                                                    }
                                                                    
                                                                    return new PaymentRejectResponse(
                                                                        payment.id,
                                                                        payment.amount,
                                                                        payment.senderName,
                                                                        payment.yapeCode,
                                                                        payment.status,
                                                                        LocalDateTime.now(),
                                                                        "Pago rechazado por vendedor " + request.sellerId() + " - " + request.reason(),
                                                                        request.sellerId(),
                                                                        LocalDateTime.now(),
                                                                        request.reason()
                                                                    );
                                                                });
                                                    });
                                        });
                            });
                })
                    .chain(response -> {
                        // Notificar resultado a todos los vendedores
                        return notifyPaymentRejectResultSimple(response.paymentId(), response.amount(), response.senderName(), 
                                response.yapeCode(), response.status(), response.message())
                                .map(v -> response);
                    });
    }
    
    /**
     * Notifica el resultado del claim a todos los vendedores (simple)
     */
    @WithTransaction
    public Uni<Void> notifyPaymentClaimResultSimple(Long adminId, Long paymentId, Double amount, String senderName, String yapeCode, String status, String message) {
        log.info("📢 Notificando resultado del pago a vendedores del admin: " + adminId);
        
        // Crear JSON directamente para WebSocket
        String notificationJson = String.format(
            "{\"type\":\"PAYMENT_CLAIMED\",\"data\":{\"paymentId\":%d,\"amount\":%.2f,\"senderName\":\"%s\",\"yapeCode\":\"%s\",\"status\":\"%s\",\"timestamp\":\"%s\",\"message\":\"%s\"}}",
            paymentId,
            amount,
            senderName,
            yapeCode,
            status,
            java.time.LocalDateTime.now(),
            message
        );
        
        return sellerRepository.find("branch.admin.id = ?1 and isActive = true", adminId).list()
                .map(sellers -> {
                    log.info("📡 Encontrados " + sellers.size() + " vendedores para notificar");
                    for (Seller seller : sellers) {
                        log.info("📱 Enviando notificación de claim a vendedor " + seller.id);
                        webSocketNotificationService.sendNotification(seller.id, notificationJson);
                    }
                    return null;
                })
                .replaceWithVoid()
                .onFailure().invoke(failure -> {
                    log.error("❌ Error al obtener vendedores para notificar: " + failure.getMessage());
                });
    }
    
    /**
     * Notifica el resultado del rechazo a todos los vendedores (simple)
     */
    @WithTransaction
    public Uni<Void> notifyPaymentRejectResultSimple(Long paymentId, Double amount, String senderName, String yapeCode, String status, String message) {
        log.info("📢 Notificando resultado del rechazo para pago: " + paymentId);
        
        // Primero obtener el adminId del pago
        return paymentNotificationRepository.findById(paymentId)
                .chain(payment -> {
                    if (payment == null) {
                        log.warn("⚠️ Pago no encontrado para notificación: " + paymentId);
                        return Uni.createFrom().voidItem();
                    }
                    
                    // Crear JSON directamente para WebSocket
                    String notificationJson = String.format(
                        "{\"type\":\"PAYMENT_REJECTED\",\"data\":{\"paymentId\":%d,\"amount\":%.2f,\"senderName\":\"%s\",\"yapeCode\":\"%s\",\"status\":\"%s\",\"timestamp\":\"%s\",\"message\":\"%s\"}}",
                        paymentId,
                        amount,
                        senderName,
                        yapeCode,
                        status,
                        java.time.LocalDateTime.now(),
                        message
                    );
                    
                    return sellerRepository.find("branch.admin.id = ?1 and isActive = true", payment.adminId).list()
                            .map(sellers -> {
                                log.info("📡 Encontrados " + sellers.size() + " vendedores para notificar");
                                for (Seller seller : sellers) {
                                    log.info("📱 Enviando notificación de rechazo a vendedor " + seller.id);
                                    webSocketNotificationService.sendNotification(seller.id, notificationJson);
                                }
                                return null;
                            })
                            .replaceWithVoid();
                })
                .onFailure().invoke(failure -> {
                    log.error("❌ Error al obtener pago para notificación: " + failure.getMessage());
                });
    }
    
    
    
    /**
     * Obtiene todos los pagos pendientes para un vendedor específico
     */
    @WithTransaction
    public Uni<List<PaymentNotificationResponse>> getPendingPaymentsForSeller(Long sellerId) {
        log.info("📋 PaymentNotificationService.getPendingPaymentsForSeller() - Obteniendo pagos pendientes para vendedor: " + sellerId);
        
        return paymentNotificationRepository.find("status in (?1, ?2)", "PENDING", "REJECTED_BY_SELLER").list()
                .chain(payments -> {
                    log.info("📋 Encontrados " + payments.size() + " pagos pendientes o rechazados por algunos");
                    
                    // Obtener los IDs de pagos que este vendedor ya rechazó
                    return paymentRejectionRepository.find("sellerId = ?1", sellerId).list()
                            .map(rejections -> {
                                java.util.Set<Long> rejectedPaymentIds = rejections.stream()
                                        .map(r -> r.paymentNotificationId)
                                        .collect(java.util.stream.Collectors.toSet());
                                
                                log.info("📋 Vendedor " + sellerId + " ya rechazó " + rejectedPaymentIds.size() + " pagos");
                                
                                // Filtrar pagos que el vendedor no ha rechazado Y que están realmente pendientes
                                return payments.stream()
                                        .filter(payment -> {
                                            // Excluir pagos que este vendedor ya rechazó
                                            if (rejectedPaymentIds.contains(payment.id)) {
                                                return false;
                                            }
                                            
                                            // Solo incluir pagos que están realmente pendientes
                                            return "PENDING".equals(payment.status);
                                        })
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
                });
    }
    
    /**
     * Obtiene los pagos pendientes para un vendedor específico con paginación
     */
    @WithTransaction
    public Uni<PendingPaymentsResponse> getPendingPaymentsForSellerPaginated(Long sellerId, int page, int size, LocalDate startDate, LocalDate endDate) {
        log.info("📋 PaymentNotificationService.getPendingPaymentsForSellerPaginated() - Obteniendo pagos pendientes paginados para vendedor: " + sellerId);
        log.info("📋 Página: " + page + ", Tamaño: " + size);
        log.info("📋 Desde: " + startDate + ", Hasta: " + endDate);
        
        // Validar parámetros de paginación
        final int validatedPage = Math.max(0, page);
        final int validatedSize = (size <= 0 || size > 100) ? 20 : size;
        
        // Obtener todos los pagos que están realmente pendientes (no confirmados ni rechazados por todos) con filtro de fechas
        return paymentNotificationRepository.find("status in (?1, ?2) and createdAt >= ?3 and createdAt <= ?4", 
                "PENDING", "REJECTED_BY_SELLER", startDate.atStartOfDay(), endDate.atTime(23, 59, 59)).list()
                .chain(payments -> {
                    log.info("📋 Encontrados " + payments.size() + " pagos pendientes o rechazados por algunos");
                    
                    // Obtener los IDs de pagos que este vendedor ya rechazó
                    return paymentRejectionRepository.find("sellerId = ?1", sellerId).list()
                            .map(rejections -> {
                                java.util.Set<Long> rejectedPaymentIds = rejections.stream()
                                        .map(r -> r.paymentNotificationId)
                                        .collect(java.util.stream.Collectors.toSet());
                                
                                log.info("📋 Vendedor " + sellerId + " ya rechazó " + rejectedPaymentIds.size() + " pagos");
                                
                                // Filtrar pagos que el vendedor no ha rechazado Y que están realmente pendientes
                                List<PaymentNotification> filteredPayments = payments.stream()
                                        .filter(payment -> {
                                            // Excluir pagos que este vendedor ya rechazó
                                            if (rejectedPaymentIds.contains(payment.id)) {
                                                return false;
                                            }
                                            
                                            // Solo incluir pagos que están realmente pendientes
                                            // (no confirmados por nadie y no rechazados por todos)
                                            return "PENDING".equals(payment.status);
                                        })
                                        .sorted((p1, p2) -> p2.createdAt.compareTo(p1.createdAt)) // Ordenar por fecha descendente
                                        .collect(java.util.stream.Collectors.toList());
                                
                                // Calcular paginación
                                int totalCount = filteredPayments.size();
                                int totalPages = (int) Math.ceil((double) totalCount / validatedSize);
                                int currentPage = Math.min(validatedPage, Math.max(0, totalPages - 1));
                                
                                // Aplicar paginación
                                int startIndex = currentPage * validatedSize;
                                int endIndex = Math.min(startIndex + validatedSize, totalCount);
                                
                                List<PaymentNotification> paginatedPayments = filteredPayments.subList(startIndex, endIndex);
                                
                                log.info("📋 Encontrados " + paginatedPayments.size() + " pagos en página " + currentPage);
                                
                                List<PaymentNotificationResponse> paymentResponses = paginatedPayments.stream()
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
    public Uni<PendingPaymentsResponse> getAllPendingPaymentsPaginated(int page, int size, LocalDate startDate, LocalDate endDate) {
        log.info("📋 PaymentNotificationService.getAllPendingPaymentsPaginated() - Obteniendo todos los pagos pendientes");
        log.info("📋 Página: " + page + ", Tamaño: " + size);
        log.info("📋 Desde: " + startDate + ", Hasta: " + endDate);

        // Validar parámetros de paginación
        final int validatedPage = Math.max(0, page);
        final int validatedSize = (size <= 0 || size > 100) ? 20 : size;

        // Obtener el total de pagos pendientes con filtro de fechas
        return paymentNotificationRepository.count("status = ?1 and createdAt >= ?2 and createdAt <= ?3", 
                "PENDING", startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
                .chain(totalCount -> {
                    log.info("📋 Total de pagos pendientes: " + totalCount);

                    // Calcular información de paginación
                    int totalPages = (int) Math.ceil((double) totalCount / validatedSize);
                    int currentPage = Math.min(validatedPage, Math.max(0, totalPages - 1));

                    // Obtener los pagos paginados con filtro de fechas
                    return paymentNotificationRepository.find("status = ?1 and createdAt >= ?2 and createdAt <= ?3 order by createdAt desc", 
                            "PENDING", startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
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
    public Uni<AdminPaymentManagementResponse> getAdminPaymentManagement(Long adminId, int page, int size, String status, LocalDate startDate, LocalDate endDate) {
        log.info("👑 PaymentNotificationService.getAdminPaymentManagement() - AdminId: " + adminId + 
                ", Página: " + page + ", Tamaño: " + size + ", Status: " + status);
        log.info("👑 Desde: " + startDate + ", Hasta: " + endDate);
        
        // Construir query basada en filtros
        String query = "adminId = ?1 and createdAt >= ?2 and createdAt <= ?3";
        List<Object> params = new ArrayList<>();
        params.add(adminId);
        params.add(startDate.atStartOfDay());
        params.add(endDate.atTime(23, 59, 59));
        
        if (status != null && !status.isEmpty()) {
            query += " and status = ?4";
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
    
    /**
     * Métodos auxiliares para manejo de timers
     */
    private Long getTimerId(Long sellerId) {
        return timerIds.get(sellerId);
    }
    
    private void setTimerId(Long sellerId, Long timerId) {
        timerIds.put(sellerId, timerId);
    }
    
    /**
     * Limpia recursos cuando un vendedor se desconecta
     */
    public void cleanupSellerResources(Long sellerId) {
        log.info("🧹 Limpiando recursos para vendedor " + sellerId);
        
        // Cancelar timer si existe
        Long timerId = getTimerId(sellerId);
        if (timerId != null) {
            vertx.cancelTimer(timerId);
        }
        
        // Limpiar colas y contadores
        notificationQueue.remove(sellerId);
        notificationCounters.remove(sellerId);
        lastNotificationTime.remove(sellerId);
        timerIds.remove(sellerId);
        
        log.info("✅ Recursos limpiados para vendedor " + sellerId);
    }
    
    /**
     * Obtiene estadísticas de notificaciones en cola
     */
    public Map<String, Object> getNotificationQueueStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalQueues", notificationQueue.size());
        stats.put("totalNotificationsInQueue", notificationQueue.values().stream().mapToInt(List::size).sum());
        stats.put("activeTimers", timerIds.size());
        stats.put("groupingWindowMs", GROUPING_WINDOW_MS);
        stats.put("maxNotificationsPerGroup", MAX_NOTIFICATIONS_PER_GROUP);
        
        return stats;
    }
    
    /**
     * Obtiene información de vendedores conectados para un admin específico
     */
    @WithTransaction
    public Uni<java.util.List<ConnectedSellerInfo>> getConnectedSellersForAdmin(Long adminId) {
        log.info("📡 PaymentNotificationService.getConnectedSellersForAdmin() - AdminId: " + adminId);
        
        // Obtener todos los vendedores del admin
        return sellerRepository.find("branch.admin.id = ?1 and isActive = true", adminId).list()
                .map(sellers -> {
                    log.info("📡 Encontrados " + sellers.size() + " vendedores activos para admin " + adminId);
                    
                    // Obtener IDs de vendedores conectados
                    java.util.Set<Long> connectedSellerIds = webSocketNotificationService.getConnectedSellerIds();
                    
                    // Crear lista de información de vendedores conectados
                    java.util.List<ConnectedSellerInfo> connectedSellers = new java.util.ArrayList<>();
                    
                    for (Seller seller : sellers) {
                        boolean isConnected = connectedSellerIds.contains(seller.id);
                        if (isConnected) {
                            ConnectedSellerInfo info = new ConnectedSellerInfo(
                                seller.id,
                                seller.sellerName,
                                seller.email,
                                seller.phone,
                                seller.branch.id,
                                seller.branch.name,
                                true,
                                java.time.LocalDateTime.now()
                            );
                            connectedSellers.add(info);
                        }
                    }
                    
                    log.info("📡 " + connectedSellers.size() + " vendedores conectados de " + sellers.size() + " totales");
                    return connectedSellers;
                });
    }
    
    /**
     * Obtiene información completa de todos los vendedores (conectados y desconectados) para un admin específico
     */
    @WithTransaction
    public Uni<java.util.List<ConnectedSellerInfo>> getAllSellersStatusForAdmin(Long adminId) {
        log.info("📡 PaymentNotificationService.getAllSellersStatusForAdmin() - AdminId: " + adminId);
        
        // Obtener todos los vendedores del admin
        return sellerRepository.find("branch.admin.id = ?1 and isActive = true", adminId).list()
                .map(sellers -> {
                    log.info("📡 Encontrados " + sellers.size() + " vendedores activos para admin " + adminId);
                    
                    // Obtener IDs de vendedores conectados
                    java.util.Set<Long> connectedSellerIds = webSocketNotificationService.getConnectedSellerIds();
                    
                    // Crear lista de información de todos los vendedores
                    java.util.List<ConnectedSellerInfo> allSellers = new java.util.ArrayList<>();
                    
                    for (Seller seller : sellers) {
                        boolean isConnected = connectedSellerIds.contains(seller.id);
                        ConnectedSellerInfo info = new ConnectedSellerInfo(
                            seller.id,
                            seller.sellerName,
                            seller.email,
                            seller.phone,
                            seller.branch.id,
                            seller.branch.name,
                            isConnected,
                            java.time.LocalDateTime.now()
                        );
                        allSellers.add(info);
                    }
                    
                    // Ordenar por estado de conexión (conectados primero)
                    allSellers.sort((s1, s2) -> Boolean.compare(s2.isConnected, s1.isConnected));
                    
                    log.info("📡 " + connectedSellerIds.size() + " vendedores conectados de " + sellers.size() + " totales");
                    return allSellers;
                });
    }
    
    /**
     * Obtiene pagos confirmados por un vendedor específico con paginación
     */
    @WithTransaction
    public Uni<PendingPaymentsResponse> getConfirmedPaymentsForSellerPaginated(Long sellerId, int page, int size, LocalDate startDate, LocalDate endDate) {
        log.info("✅ PaymentNotificationService.getConfirmedPaymentsForSellerPaginated() - SellerId: " + sellerId);
        log.info("✅ Página: " + page + ", Tamaño: " + size);
        log.info("✅ Desde: " + startDate + ", Hasta: " + endDate);
        
        // Calcular offset
        int offset = page * size;
        
        // Obtener pagos confirmados por este vendedor con filtro de fechas
        return paymentNotificationRepository.find(
                "confirmedBy = ?1 and status = ?2 and createdAt >= ?3 and createdAt <= ?4 order by confirmedAt desc", 
                sellerId, "CONFIRMED", startDate.atStartOfDay(), endDate.atTime(23, 59, 59)
            )
            .page(offset, size)
            .list()
            .chain(payments -> {
                log.info("✅ Encontrados " + payments.size() + " pagos confirmados para seller " + sellerId);
                
                // Contar total de pagos confirmados
                return paymentNotificationRepository.count(
                    "confirmedBy = ?1 and status = ?2", 
                    sellerId, "CONFIRMED"
                )
                .map(totalCount -> {
                    // Convertir a DTOs
                    List<PaymentNotificationResponse> paymentResponses = payments.stream()
                        .map(payment -> new PaymentNotificationResponse(
                            payment.id,
                            payment.amount,
                            payment.senderName,
                            payment.yapeCode,
                            payment.status,
                            payment.confirmedAt != null ? payment.confirmedAt : payment.createdAt,
                            "Pago confirmado por vendedor " + sellerId
                        ))
                        .collect(Collectors.toList());
                    
                    // Calcular información de paginación
                    int totalPages = (int) Math.ceil((double) totalCount / size);
                    
                    PendingPaymentsResponse.PaginationInfo pagination = new PendingPaymentsResponse.PaginationInfo(
                        page, totalPages, totalCount, size
                    );
                    
                    return new PendingPaymentsResponse(paymentResponses, pagination);
                });
            });
    }

  /**
   * Clase interna para información de vendedores conectados
   */
  public record ConnectedSellerInfo(Long sellerId, String sellerName, String email, String phone, Long branchId,
                                    String branchName, boolean isConnected, LocalDateTime lastSeen) {
  }
    
}
