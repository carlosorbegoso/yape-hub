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
import org.sky.dto.payment.PaymentNotificationRequest;
import org.sky.model.Notification;
import org.sky.model.YapeNotification;
import org.sky.model.Transaction;
import org.sky.repository.NotificationRepository;
import org.sky.repository.YapeNotificationRepository;
import org.sky.repository.TransactionRepository;
import org.sky.repository.BranchRepository;
import org.sky.exception.ValidationException;
import org.sky.service.PaymentNotificationService;
import org.sky.controller.PaymentWebSocketController;
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

  @Inject
  PaymentNotificationService paymentNotificationService;

  @Inject
  PaymentWebSocketController webSocketController;

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

      // Crear notificación de pago (usando la lógica que funciona)
      PaymentNotificationRequest paymentRequest = new PaymentNotificationRequest(
          request.adminId(),
          decryptedResponse.amount(),
          decryptedResponse.senderName(), // Usar el nombre real del remitente
          decryptedResponse.transactionId()
      );

      // Procesar como notificación de pago
      return paymentNotificationService.processPaymentNotification(paymentRequest)
          .map(paymentResponse -> {
            log.info("✅ Notificación de Yape procesada exitosamente");

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

    } catch (Exception e) {
      log.error("❌ Error procesando notificación de Yape: " + e.getMessage());
      throw ValidationException.invalidField("encryptedNotification", request.encryptedNotification(),
          "Error procesando notificación encriptada: " + e.getMessage());
    }
  }

  /**
   * Procesa notificación de Yape y la convierte en notificación de pago para broadcast
   */
  @WithTransaction
  public Uni<ApiResponse<YapeNotificationResponse>> processYapeNotificationAsPayment(YapeNotificationRequest request) {
    log.info("💰 NotificationService.processYapeNotificationAsPayment() - Procesando como pago");

    try {
      // Desencriptar notificación
      YapeNotificationResponse decryptedResponse = yapeDecryptionService.decryptYapeNotification(
          request.encryptedNotification(),
          request.deviceFingerprint()
      );

      // Crear notificación de pago
      PaymentNotificationRequest paymentRequest = new PaymentNotificationRequest(
          request.adminId(),
          decryptedResponse.amount(),
          decryptedResponse.senderPhone(), // Usar como nombre por ahora
          decryptedResponse.transactionId()
      );

      // Procesar como notificación de pago
      return paymentNotificationService.processPaymentNotification(paymentRequest)
          .map(paymentResponse -> {
            log.info("✅ Notificación de Yape procesada como pago");

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
                "Pago enviado a vendedores para confirmación"
            );

            return ApiResponse.success("Notificación de Yape procesada como pago", yapeResponse);
          });

    } catch (Exception e) {
      log.error("❌ Error procesando notificación de Yape como pago: " + e.getMessage());
      throw ValidationException.invalidField("encryptedNotification", request.encryptedNotification(),
          "Error procesando notificación como pago: " + e.getMessage());
    }
  }

}