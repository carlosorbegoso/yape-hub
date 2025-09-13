package org.sky.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import org.sky.dto.ApiResponse;
import org.sky.dto.notification.NotificationResponse;
import org.sky.dto.notification.SendNotificationRequest;
import org.sky.dto.notification.YapeNotificationRequest;
import org.sky.dto.notification.YapeNotificationResponse;
import org.sky.model.Notification;
import org.sky.model.YapeNotification;
import org.sky.model.Transaction;
import org.sky.repository.NotificationRepository;
import org.sky.repository.YapeNotificationRepository;
import org.sky.repository.TransactionRepository;
import org.sky.repository.BranchRepository;
import org.sky.exception.ValidationException;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class NotificationService {
    
    @Inject
    NotificationRepository notificationRepository;
    
    @Inject
    YapeNotificationRepository yapeNotificationRepository;
    
    @Inject
    TransactionRepository transactionRepository;
    
    @Inject
    BranchRepository branchRepository;
    
    @Inject
    YapeDecryptionService yapeDecryptionService;
    
    @Inject
    DeviceFingerprintService deviceFingerprintService;
    
    private static final Logger log = Logger.getLogger(NotificationService.class);
    
    @WithTransaction
    public Uni<ApiResponse<String>> sendNotification(SendNotificationRequest request) {
        Notification notification = new Notification();
        notification.targetType = request.targetType();
        notification.targetId = request.targetId();
        notification.title = request.title();
        notification.message = request.message();
        notification.type = request.type();
        notification.data = request.data();
        
        return notificationRepository.persist(notification)
                .map(persistedNotification -> {
                    // TODO: Send push notification to mobile devices
                    // TODO: Send email notification if configured
                    return ApiResponse.success("Notificación enviada exitosamente");
                });
    }
    
    public Uni<ApiResponse<List<NotificationResponse>>> getNotifications(Long userId, String userRole, 
                                                                  int page, int limit, Boolean unreadOnly) {
        Uni<List<Notification>> notificationsUni;
        
        if ("ADMIN".equals(userRole)) {
            if (unreadOnly != null && unreadOnly) {
                notificationsUni = notificationRepository.findUnreadByTargetTypeAndTargetId(
                        Notification.TargetType.ADMIN, userId);
            } else {
                notificationsUni = notificationRepository.findByTargetTypeAndTargetId(
                        Notification.TargetType.ADMIN, userId);
            }
        } else if ("SELLER".equals(userRole)) {
            if (unreadOnly != null && unreadOnly) {
                notificationsUni = notificationRepository.findUnreadByTargetTypeAndTargetId(
                        Notification.TargetType.SELLER, userId);
            } else {
                notificationsUni = notificationRepository.findByTargetTypeAndTargetId(
                        Notification.TargetType.SELLER, userId);
            }
        } else {
            // Get all notifications for user
            Uni<List<Notification>> adminNotificationsUni = notificationRepository.findByTargetTypeAndTargetId(
                    Notification.TargetType.ADMIN, userId);
            Uni<List<Notification>> sellerNotificationsUni = notificationRepository.findByTargetTypeAndTargetId(
                    Notification.TargetType.SELLER, userId);
            
            notificationsUni = Uni.combine().all().unis(adminNotificationsUni, sellerNotificationsUni)
                    .with((adminNotifications, sellerNotifications) -> {
                        List<Notification> allNotifications = new java.util.ArrayList<>();
                        allNotifications.addAll(adminNotifications);
                        allNotifications.addAll(sellerNotifications);
                        return allNotifications;
                    });
        }
        
        return notificationsUni.map(notifications -> {
            // Pagination
            int totalItems = notifications.size();
            int startIndex = (page - 1) * limit;
            int endIndex = Math.min(startIndex + limit, totalItems);
            
            List<Notification> paginatedNotifications = notifications.subList(startIndex, endIndex);
            
            List<NotificationResponse> responses = paginatedNotifications.stream()
                    .map(notification -> new NotificationResponse(
                            notification.id, notification.targetType, notification.targetId,
                            notification.title, notification.message, notification.type,
                            notification.data, notification.isRead, notification.readAt, notification.createdAt
                    ))
                    .collect(Collectors.toList());
            
            return ApiResponse.success("Notificaciones obtenidas exitosamente", responses);
        });
    }
    
    @WithTransaction
    public Uni<ApiResponse<String>> markNotificationAsRead(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .chain(notification -> {
                    if (notification == null) {
                        return Uni.createFrom().item(ApiResponse.<String>error("Notificación no encontrada"));
                    }
                    
                    notification.isRead = true;
                    notification.readAt = LocalDateTime.now();
                    
                    return notificationRepository.persist(notification)
                            .map(persistedNotification -> ApiResponse.success("Notificación marcada como leída"));
                });
    }
    
    @WithTransaction
    public Uni<ApiResponse<YapeNotificationResponse>> processYapeNotification(YapeNotificationRequest request) {
        log.info("🔐 NotificationService.processYapeNotification() - Procesando notificación encriptada de Yape");
        log.info("🔐 AdminId: " + request.adminId());
        log.info("🔐 Device fingerprint: " + request.deviceFingerprint());
        log.info("🔐 Timestamp: " + request.timestamp());
        
        try {
            // Validar timestamp (no debe ser muy antiguo)
            long currentTime = System.currentTimeMillis();
            long timeDiff = Math.abs(currentTime - request.timestamp());
            long maxTimeDiff = 5 * 60 * 1000; // 5 minutos en milisegundos
            
            if (timeDiff > maxTimeDiff) {
                log.warn("❌ Timestamp muy antiguo: " + timeDiff + "ms");
                throw ValidationException.invalidField("timestamp", request.timestamp().toString(), 
                    "Timestamp muy antiguo. Diferencia: " + timeDiff + "ms");
            }
            
            // Validar device fingerprint
            deviceFingerprintService.validateDeviceFingerprint(request.deviceFingerprint());
            
            // Crear registro de notificación de Yape
            YapeNotification yapeNotification = new YapeNotification();
            yapeNotification.adminId = request.adminId();
            yapeNotification.encryptedNotification = request.encryptedNotification();
            yapeNotification.deviceFingerprint = request.deviceFingerprint();
            yapeNotification.timestamp = request.timestamp();
            yapeNotification.isProcessed = false;
            
            // Guardar notificación
            return yapeNotificationRepository.persist(yapeNotification)
                    .chain(savedNotification -> {
                        log.info("💾 Notificación de Yape guardada con ID: " + savedNotification.id);
                        
                        // Desencriptar notificación
                        YapeNotificationResponse decryptedResponse = yapeDecryptionService.decryptYapeNotification(
                            request.encryptedNotification(), 
                            request.deviceFingerprint()
                        );
                        
                        // Obtener la branch del admin
                        return branchRepository.find("admin.id", request.adminId()).firstResult()
                                .chain(branch -> {
                                    if (branch == null) {
                                        throw ValidationException.invalidField("adminId", request.adminId().toString(), 
                                            "No se encontró una sucursal para el administrador");
                                    }
                                    
                                    // Crear transacción
                                    Transaction transaction = new Transaction();
                                    transaction.transactionId = decryptedResponse.transactionId();
                                    transaction.amount = decryptedResponse.amount();
                                    transaction.senderPhone = decryptedResponse.senderPhone();
                                    transaction.receiverPhone = decryptedResponse.receiverPhone();
                                    transaction.status = decryptedResponse.status();
                                    transaction.adminId = request.adminId();
                                    transaction.yapeNotificationId = savedNotification.id;
                                    transaction.branch = branch;
                                    
                                    // Verificar si ya existe una transacción con el mismo código
                                    return transactionRepository.find("transactionId", decryptedResponse.transactionId()).firstResult()
                                            .chain(existingTransaction -> {
                                                if (existingTransaction != null) {
                                                    log.warn("⚠️ Transacción duplicada detectada: " + decryptedResponse.transactionId());
                                                    
                                                    // Marcar notificación como procesada pero con error de duplicado
                                                    savedNotification.isProcessed = true;
                                                    savedNotification.processedAt = LocalDateTime.now();
                                                    savedNotification.transactionId = decryptedResponse.transactionId();
                                                    savedNotification.amount = decryptedResponse.amount();
                                                    savedNotification.senderPhone = decryptedResponse.senderPhone();
                                                    savedNotification.receiverPhone = decryptedResponse.receiverPhone();
                                                    savedNotification.status = "DUPLICATE";
                                                    
                                                    return yapeNotificationRepository.persist(savedNotification)
                                                            .map(updatedNotification -> {
                                                                log.info("✅ Notificación guardada como duplicada");
                                                                
                                                                YapeNotificationResponse response = new YapeNotificationResponse(
                                                                    updatedNotification.id,
                                                                    decryptedResponse.transactionId(),
                                                                    decryptedResponse.amount(),
                                                                    decryptedResponse.senderPhone(),
                                                                    decryptedResponse.receiverPhone(),
                                                                    "DUPLICATE",
                                                                    updatedNotification.processedAt,
                                                                    "Transacción duplicada - solo se guardó la notificación"
                                                                );
                                                                
                                                                return ApiResponse.success("Notificación guardada (transacción duplicada)", response);
                                                            });
                                                } else {
                                                    // Guardar nueva transacción
                                                    return transactionRepository.persist(transaction)
                                                            .chain(savedTransaction -> {
                                                                log.info("💾 Transacción guardada con ID: " + savedTransaction.id);
                                                                
                                                                // Actualizar notificación como procesada
                                                                savedNotification.isProcessed = true;
                                                                savedNotification.processedAt = LocalDateTime.now();
                                                                savedNotification.transactionId = decryptedResponse.transactionId();
                                                                savedNotification.amount = decryptedResponse.amount();
                                                                savedNotification.senderPhone = decryptedResponse.senderPhone();
                                                                savedNotification.receiverPhone = decryptedResponse.receiverPhone();
                                                                savedNotification.status = decryptedResponse.status();
                                                                
                                                                return yapeNotificationRepository.persist(savedNotification)
                                                                        .map(updatedNotification -> {
                                                                            log.info("✅ Notificación de Yape procesada exitosamente");
                                                                            
                                                                            // Crear respuesta con datos reales
                                                                            YapeNotificationResponse response = new YapeNotificationResponse(
                                                                                updatedNotification.id,
                                                                                decryptedResponse.transactionId(),
                                                                                decryptedResponse.amount(),
                                                                                decryptedResponse.senderPhone(),
                                                                                decryptedResponse.receiverPhone(),
                                                                                decryptedResponse.status(),
                                                                                updatedNotification.processedAt,
                                                                                "Transacción procesada y guardada exitosamente"
                                                                            );
                                                                            
                                                                            return ApiResponse.success("Notificación de Yape procesada exitosamente", response);
                                                                        });
                                                            });
                                                }
                                            });
                                });
                    });
            
        } catch (Exception e) {
            log.error("❌ Error procesando notificación de Yape: " + e.getMessage());
            
            // Si es un error de constraint violation (duplicado), crear ErrorResponse específico
            if (e.getMessage() != null && e.getMessage().contains("duplicate key value violates unique constraint")) {
                throw ValidationException.invalidField("transactionId", "duplicate", 
                    "Código de transacción duplicado. La notificación se guardó pero la transacción ya existe en el sistema");
            }
            
            throw ValidationException.invalidField("encryptedNotification", request.encryptedNotification(), 
                "Error procesando notificación encriptada: " + e.getMessage());
        }
    }
}
