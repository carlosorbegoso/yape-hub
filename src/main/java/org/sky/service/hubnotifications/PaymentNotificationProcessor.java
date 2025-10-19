package org.sky.service.hubnotifications;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.response.payment.PaymentNotificationResponse;
import org.sky.service.websocket.WebSocketNotificationService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class PaymentNotificationProcessor {

    @Inject
    WebSocketNotificationService webSocketNotificationService;

    private final Map<Long, List<PaymentNotificationResponse>> notificationQueue = new ConcurrentHashMap<>();
    private final Map<Long, Long> timerIds = new ConcurrentHashMap<>();
    private final AtomicInteger processedCount = new AtomicInteger(0);


    public Uni<Void> processNotificationQueue(Long sellerId) {
        return Uni.createFrom().item(() -> {
            List<PaymentNotificationResponse> notifications = notificationQueue.remove(sellerId);
            timerIds.remove(sellerId);
            
            if (notifications != null && !notifications.isEmpty()) {
                if (notifications.size() == 1) {
                    sendIndividualNotification(sellerId, notifications.get(0));
                } else {
                    sendGroupedNotification(sellerId, notifications);
                }
                processedCount.incrementAndGet();
            }
            return null;
        });
    }

    public Uni<Void> addToQueue(Long sellerId, PaymentNotificationResponse notification) {
        return Uni.createFrom().item(() -> {
            notificationQueue.computeIfAbsent(sellerId, k -> new java.util.ArrayList<>()).add(notification);
            scheduleNotificationDelivery(sellerId);
            return null;
        });
    }

    private void sendIndividualNotification(Long sellerId, PaymentNotificationResponse notification) {
        String notificationJson = PaymentNotificationMapper.TO_INDIVIDUAL_JSON.apply(notification);
        webSocketNotificationService.sendNotificationReactive(sellerId, notificationJson)
            .subscribe()
            .with(
                success -> {},
                failure -> {}
            );
    }

    private void sendGroupedNotification(Long sellerId, List<PaymentNotificationResponse> notifications) {
        String groupedJson = PaymentNotificationMapper.TO_GROUPED_JSON.apply(notifications);
        webSocketNotificationService.sendNotificationReactive(sellerId, groupedJson)
            .subscribe()
            .with(
                success -> {},
                failure -> {}
            );
    }

    private void scheduleNotificationDelivery(Long sellerId) {
        Long existingTimerId = timerIds.get(sellerId);
        if (existingTimerId != null) {
            // Cancel existing timer - actual implementation would cancel the Vertx timer
        }
        
        // Send notification immediately for real-time delivery
        processNotificationQueue(sellerId)
            .subscribe()
            .with(
                success -> {},
                failure -> {}
            );
        
        timerIds.remove(sellerId);
    }

}
