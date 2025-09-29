package org.sky.service.websocket;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import io.vertx.core.Vertx;
import io.smallrye.mutiny.Uni;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class WebSocketNotificationService {
    
    @Inject
    Vertx vertx;
    
    private static final Logger log = Logger.getLogger(WebSocketNotificationService.class);
    
    private final Map<Long, jakarta.websocket.Session> webSocketSessions = new ConcurrentHashMap<>();
    private final Map<Long, AtomicLong> lastActivityTime = new ConcurrentHashMap<>();
    private Long cleanupTimerId;
    
    // Optimized timeouts for real-time performance
    private static final long CLEANUP_INTERVAL_MS = 10000; // Reduced from 30s to 10s
    private static final long SESSION_TIMEOUT_MS = 60000;  // Reduced from 5min to 1min
    
    public void registerSession(Long sellerId, jakarta.websocket.Session session) {
        webSocketSessions.put(sellerId, session);
        lastActivityTime.put(sellerId, new AtomicLong(System.currentTimeMillis()));
        log.info("📱 Registered WebSocket session for seller " + sellerId + ". Total sessions: " + webSocketSessions.size());
        
        if (cleanupTimerId == null) {
            startCleanupTimer();
        }
    }
    
    public void unregisterSession(Long sellerId) {
        webSocketSessions.remove(sellerId);
        lastActivityTime.remove(sellerId);
        log.info("📱 Unregistered WebSocket session for seller " + sellerId + ". Total sessions: " + webSocketSessions.size());
    }
    
    /**
     * Optimized real-time notification sending with immediate delivery
     */
    public Uni<Void> sendNotificationReactive(Long sellerId, String message) {
        return Uni.createFrom().item(() -> {
            jakarta.websocket.Session session = webSocketSessions.get(sellerId);
            if (session != null && session.isOpen()) {
                try {
                    // Send message immediately for real-time performance
                    session.getAsyncRemote().sendText(message);
                    
                    // Update activity immediately
                    AtomicLong lastActivity = lastActivityTime.get(sellerId);
                    if (lastActivity != null) {
                        lastActivity.set(System.currentTimeMillis());
                    }
                    
                    log.debug("⚡ Real-time notification sent to seller " + sellerId);
                    return null;
                } catch (Exception e) {
                    log.error("❌ Error sending real-time notification to seller " + sellerId + ": " + e.getMessage());
                    unregisterSession(sellerId);
                    throw new RuntimeException(e);
                }
            } else {
                log.warn("⚠️ Seller " + sellerId + " not connected for real-time notification");
                return null;
            }
        });
    }
    
    /**
     * Legacy method for backward compatibility - now uses reactive approach
     */
    public void sendNotification(Long sellerId, String message) {
        sendNotificationReactive(sellerId, message)
            .subscribe()
            .with(
                success -> {},
                failure -> log.error("Failed to send notification to seller " + sellerId, failure)
            );
    }
    
    
    
    public boolean isSellerConnected(Long sellerId) {
        jakarta.websocket.Session session = webSocketSessions.get(sellerId);
        return session != null && session.isOpen();
    }
    
    public int getConnectedSellersCount() {
        return webSocketSessions.size();
    }
    
    public java.util.Set<Long> getConnectedSellerIds() {
        java.util.Set<Long> connectedIds = webSocketSessions.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().isOpen())
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
        log.info("📱 getConnectedSellerIds() - Total sessions: " + webSocketSessions.size() + ", Connected: " + connectedIds.size());
        return connectedIds;
    }
    
    
    private void startCleanupTimer() {
        if (cleanupTimerId != null) {
            return;
        }
        
        cleanupTimerId = vertx.setPeriodic(CLEANUP_INTERVAL_MS, timerId -> {
            cleanupInactiveSessions();
        });
    }
    
    private void cleanupInactiveSessions() {
        long currentTime = System.currentTimeMillis();
        java.util.List<Long> inactiveSellers = new java.util.ArrayList<>();
        
        for (Map.Entry<Long, AtomicLong> entry : lastActivityTime.entrySet()) {
            Long sellerId = entry.getKey();
            AtomicLong lastActivity = entry.getValue();
            
            if (lastActivity != null) {
                long timeSinceLastActivity = currentTime - lastActivity.get();
                if (timeSinceLastActivity > SESSION_TIMEOUT_MS) {
                    inactiveSellers.add(sellerId);
                }
            }
        }
        
        for (Long sellerId : inactiveSellers) {
            jakarta.websocket.Session session = webSocketSessions.get(sellerId);
            if (session != null && !session.isOpen()) {
                unregisterSession(sellerId);
            }
        }
    }
    
    public void updateActivity(Long sellerId) {
        AtomicLong lastActivity = lastActivityTime.get(sellerId);
        if (lastActivity != null) {
            lastActivity.set(System.currentTimeMillis());
        }
    }
    
    /**
     * Broadcast message to multiple sellers with optimized real-time delivery
     */
    public Uni<Void> broadcastToSellersReactive(java.util.Set<Long> sellerIds, String message) {
        return Uni.createFrom().item(() -> {
            final int[] sentCount = {0};
            for (Long sellerId : sellerIds) {
                if (isSellerConnected(sellerId)) {
                    sendNotificationReactive(sellerId, message)
                        .subscribe()
                        .with(
                            success -> sentCount[0]++,
                            failure -> log.warn("Failed to broadcast to seller " + sellerId, failure)
                        );
                }
            }
            log.info("📡 Broadcast sent to " + sentCount[0] + " connected sellers");
            return null;
        });
    }
    
    /**
     * Get real-time connection status for all sellers
     */
    public java.util.Map<Long, Boolean> getRealTimeConnectionStatus() {
        java.util.Map<Long, Boolean> status = new ConcurrentHashMap<>();
        webSocketSessions.forEach((sellerId, session) -> {
            status.put(sellerId, session != null && session.isOpen());
        });
        return status;
    }
    
}
