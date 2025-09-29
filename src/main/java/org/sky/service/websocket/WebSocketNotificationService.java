package org.sky.service.websocket;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import io.vertx.core.Vertx;

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
    
    private static final long CLEANUP_INTERVAL_MS = 30000;
    private static final long SESSION_TIMEOUT_MS = 300000;
    
    public void registerSession(Long sellerId, jakarta.websocket.Session session) {
        webSocketSessions.put(sellerId, session);
        lastActivityTime.put(sellerId, new AtomicLong(System.currentTimeMillis()));
        
        if (cleanupTimerId == null) {
            startCleanupTimer();
        }
    }
    
    public void unregisterSession(Long sellerId) {
        webSocketSessions.remove(sellerId);
        lastActivityTime.remove(sellerId);
    }
    
    public void sendNotification(Long sellerId, String message) {
        jakarta.websocket.Session session = webSocketSessions.get(sellerId);
        if (session != null && session.isOpen()) {
            try {
                session.getAsyncRemote().sendText(message);
                
                AtomicLong lastActivity = lastActivityTime.get(sellerId);
                if (lastActivity != null) {
                    lastActivity.set(System.currentTimeMillis());
                }
            } catch (Exception e) {
                log.error("Error sending notification to seller " + sellerId + ": " + e.getMessage());
                unregisterSession(sellerId);
            }
        }
    }
    
    
    
    public boolean isSellerConnected(Long sellerId) {
        jakarta.websocket.Session session = webSocketSessions.get(sellerId);
        return session != null && session.isOpen();
    }
    
    public int getConnectedSellersCount() {
        return webSocketSessions.size();
    }
    
    public java.util.Set<Long> getConnectedSellerIds() {
        return webSocketSessions.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().isOpen())
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
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
    
}
