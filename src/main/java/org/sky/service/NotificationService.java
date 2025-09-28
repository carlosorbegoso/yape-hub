package org.sky.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import org.sky.dto.ApiResponse;
import org.sky.dto.notification.NotificationResponse;
import org.sky.dto.notification.YapeNotificationRequest;
import org.sky.dto.notification.YapeNotificationResponse;
import org.sky.dto.notification.YapeAuditResponse;
import org.sky.dto.payment.PaymentNotificationRequest;
import org.sky.model.Notification;
import org.sky.model.YapeNotificationAudit;
import org.sky.repository.NotificationRepository;
import org.sky.repository.YapeNotificationAuditRepository;
import org.sky.exception.ValidationException;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class NotificationService {

  @Inject
  NotificationRepository notificationRepository;


  @Inject
  YapeNotificationAuditRepository yapeNotificationAuditRepository;

  @Inject
  YapeDecryptionService yapeDecryptionService;

  @Inject
  DeviceFingerprintService deviceFingerprintService;

  @Inject
  PaymentNotificationService paymentNotificationService;


  private static final Logger log = Logger.getLogger(NotificationService.class);

  public Uni<ApiResponse<List<NotificationResponse>>> getNotifications(Long userId, String userRole,
                                                                       int page, int limit, Boolean unreadOnly, LocalDate startDate, LocalDate endDate) {
    log.info("🔔 NotificationService.getNotifications() - UserId: " + userId + ", Role: " + userRole);
    log.info("🔔 Desde: " + startDate + ", Hasta: " + endDate);
    
    Uni<List<Notification>> notificationsUni;

    if ("ADMIN".equals(userRole)) {
      if (unreadOnly != null && unreadOnly) {
        notificationsUni = notificationRepository.find("targetType = ?1 and targetId = ?2 and isRead = false and createdAt >= ?3 and createdAt <= ?4 order by createdAt desc", 
            Notification.TargetType.ADMIN, userId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59)).list();
      } else {
        notificationsUni = notificationRepository.find("targetType = ?1 and targetId = ?2 and createdAt >= ?3 and createdAt <= ?4 order by createdAt desc", 
            Notification.TargetType.ADMIN, userId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59)).list();
      }
    } else if ("SELLER".equals(userRole)) {
      if (unreadOnly != null && unreadOnly) {
        notificationsUni = notificationRepository.find("targetType = ?1 and targetId = ?2 and isRead = false and createdAt >= ?3 and createdAt <= ?4 order by createdAt desc", 
            Notification.TargetType.SELLER, userId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59)).list();
      } else {
        notificationsUni = notificationRepository.find("targetType = ?1 and targetId = ?2 and createdAt >= ?3 and createdAt <= ?4 order by createdAt desc", 
            Notification.TargetType.SELLER, userId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59)).list();
      }
    } else {
      // Get all notifications for user
      Uni<List<Notification>> adminNotificationsUni = notificationRepository.find("targetType = ?1 and targetId = ?2 and createdAt >= ?3 and createdAt <= ?4 order by createdAt desc", 
          Notification.TargetType.ADMIN, userId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59)).list();
      Uni<List<Notification>> sellerNotificationsUni = notificationRepository.find("targetType = ?1 and targetId = ?2 and createdAt >= ?3 and createdAt <= ?4 order by createdAt desc", 
          Notification.TargetType.SELLER, userId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59)).list();

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
    log.info("🔐 Deduplication Hash: " + request.deduplicationHash());

    // Crear registro de auditoría ANTES de procesar
    YapeNotificationAudit auditRecord = new YapeNotificationAudit();
    auditRecord.adminId = request.adminId();
    auditRecord.encryptedNotification = request.encryptedNotification();
    auditRecord.deviceFingerprint = request.deviceFingerprint();
    auditRecord.timestamp = request.timestamp();
    auditRecord.deduplicationHash = request.deduplicationHash();
    auditRecord.decryptionStatus = "PENDING";

    return yapeNotificationAuditRepository.persist(auditRecord)
        .chain(savedAudit -> {
          log.info("📋 Registro de auditoría creado con ID: " + savedAudit.id);
          
          try {
            // Validar timestamp (no debe ser muy antiguo)
            long currentTime = System.currentTimeMillis();
            long timeDiff = Math.abs(currentTime - request.timestamp());
            long maxTimeDiff = 5 * 60 * 1000; // 5 minutos en milisegundos

            if (timeDiff > maxTimeDiff) {
              log.warn("❌ Timestamp muy antiguo: " + timeDiff + "ms");
              // Actualizar auditoría con error
              savedAudit.decryptionStatus = "FAILED";
              savedAudit.decryptionError = "Timestamp muy antiguo. Diferencia: " + timeDiff + "ms";
              return yapeNotificationAuditRepository.persist(savedAudit)
                  .replaceWith(Uni.createFrom().failure(
                      ValidationException.invalidField("timestamp", request.timestamp().toString(),
                          "Timestamp muy antiguo. Diferencia: " + timeDiff + "ms")
                  ));
            }

            // Validar device fingerprint
            deviceFingerprintService.validateDeviceFingerprint(request.deviceFingerprint());

            // Desencriptar notificación
            YapeNotificationResponse decryptedResponse = yapeDecryptionService.decryptYapeNotification(
                request.encryptedNotification(),
                request.deviceFingerprint()
            );

            log.info("✅ Notificación desencriptada exitosamente");
            log.info("✅ Transaction ID: " + decryptedResponse.transactionId());
            log.info("✅ Amount: " + decryptedResponse.amount());
            log.info("✅ Sender Phone: " + decryptedResponse.senderPhone());
            log.info("✅ Sender Name: " + decryptedResponse.senderName());
            log.info("✅ Receiver: " + decryptedResponse.receiverPhone());

            // Actualizar auditoría con datos extraídos
            savedAudit.decryptionStatus = "SUCCESS";
            savedAudit.extractedAmount = decryptedResponse.amount();
            savedAudit.extractedSenderName = decryptedResponse.senderName();
            savedAudit.extractedYapeCode = decryptedResponse.transactionId().replace("YAPE_", "");
            savedAudit.transactionId = decryptedResponse.transactionId();

            // Crear notificación de pago (usando la lógica que funciona)
            PaymentNotificationRequest paymentRequest = new PaymentNotificationRequest(
                request.adminId(),
                decryptedResponse.amount(),
                decryptedResponse.senderName(), // Usar el nombre real del remitente
                decryptedResponse.transactionId(),
                request.deduplicationHash() // Pasar el hash de deduplicación
            );

            // Procesar como notificación de pago
            return paymentNotificationService.processPaymentNotification(paymentRequest)
                .chain(paymentResponse -> {
                  log.info("✅ Notificación de Yape procesada exitosamente");

                  // Actualizar auditoría con ID del pago
                  savedAudit.paymentNotificationId = paymentResponse.paymentId();
                  return yapeNotificationAuditRepository.persist(savedAudit)
                      .map(updatedAudit -> {
                        log.info("📋 Auditoría actualizada con Payment ID: " + paymentResponse.paymentId());

                        // Crear respuesta de Yape con información del pago
                        YapeNotificationResponse yapeResponse = new YapeNotificationResponse(
                            paymentResponse.paymentId(),
                            decryptedResponse.transactionId(),
                            decryptedResponse.amount(),
                            decryptedResponse.senderPhone(),
                            decryptedResponse.senderName(),
                            decryptedResponse.receiverPhone(),
                            "PENDING_CONFIRMATION",
                            paymentResponse.timestamp(),
                            "Transacción procesada y enviada a vendedores para confirmación"
                        );

                        return ApiResponse.success("Notificación de Yape procesada exitosamente", yapeResponse);
                      });
                });

          } catch (Exception e) {
            log.error("❌ Error procesando notificación de Yape: " + e.getMessage());
            
            // Actualizar auditoría con error
            savedAudit.decryptionStatus = "FAILED";
            savedAudit.decryptionError = e.getMessage();
            return yapeNotificationAuditRepository.persist(savedAudit)
                .replaceWith(Uni.createFrom().failure(
                    ValidationException.invalidField("encryptedNotification", request.encryptedNotification(),
                        "Error procesando notificación encriptada: " + e.getMessage())
                ));
          }
        });
  }

  /**
   * Obtiene el historial de auditoría de notificaciones de Yape para un admin
   */
  @WithTransaction
  public Uni<ApiResponse<java.util.List<YapeAuditResponse>>> getYapeNotificationAudit(Long adminId, int page, int size) {
    log.info("📋 NotificationService.getYapeNotificationAudit() - AdminId: " + adminId + ", Página: " + page + ", Tamaño: " + size);
    
    // Validar parámetros de paginación
    final int validatedPage = Math.max(0, page);
    final int validatedSize = (size <= 0 || size > 100) ? 20 : size;
    
    return yapeNotificationAuditRepository.findByAdminId(adminId)
        .map(auditRecords -> {
          // Aplicar paginación
          int totalCount = auditRecords.size();
          int startIndex = validatedPage * validatedSize;
          int endIndex = Math.min(startIndex + validatedSize, totalCount);
          
          List<YapeNotificationAudit> paginatedRecords = auditRecords.subList(startIndex, endIndex);
          
          // Convertir a DTOs
          List<YapeAuditResponse> auditResponses = paginatedRecords.stream()
              .map(audit -> new YapeAuditResponse(
                  audit.id,
                  audit.adminId,
                  audit.encryptedNotification,
                  audit.deviceFingerprint,
                  audit.timestamp,
                  audit.deduplicationHash,
                  audit.decryptionStatus,
                  audit.decryptionError,
                  audit.extractedAmount,
                  audit.extractedSenderName,
                  audit.extractedYapeCode,
                  audit.transactionId,
                  audit.paymentNotificationId,
                  audit.createdAt,
                  audit.updatedAt
              ))
              .collect(Collectors.toList());
          
          log.info("📋 Encontrados " + auditResponses.size() + " registros de auditoría para admin " + adminId);
          return ApiResponse.success("Auditoría de Yape obtenida exitosamente", auditResponses);
        });
  }

}