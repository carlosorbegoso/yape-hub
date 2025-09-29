package org.sky.service.websocket;

import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.Session;
import org.jboss.logging.Logger;
import org.sky.service.websocket.WebSocketNotificationService;

@ApplicationScoped
public class WebSocketMessageHandler {

    @Inject
    Vertx vertx;
    
    @Inject
    WebSocketNotificationService webSocketNotificationService;

    private static final Logger log = Logger.getLogger(WebSocketMessageHandler.class);

    public Uni<Void> handleMessage(String message, Session session, String sellerIdParam) {
        return Uni.createFrom().item(() -> {
            try {
                Long sellerId = Long.parseLong(sellerIdParam);
                
                // Validate message
                if (message == null || message.trim().isEmpty()) {
                    log.warn("Empty message received from seller " + sellerId);
                    return null;
                }
                
                if (message.length() > 8192) { // 8KB
                    log.warn("Message too large from seller " + sellerId + ": " + message.length() + " bytes");
                    return null;
                }
                
                // Validate session is still open
                if (!session.isOpen()) {
                    log.warn("Attempted to send message to closed session for seller " + sellerId);
                    return null;
                }
                
                return sellerId;
            } catch (NumberFormatException e) {
                log.error("Invalid seller ID in message handler: " + sellerIdParam);
                return null;
            }
        })
        .chain(sellerId -> {
            if (sellerId == null) {
                return Uni.createFrom().item(null);
            }
            
            return Uni.combine().all().unis(
                    updateActivity(sellerId),
                    processMessage(message, session, sellerId)
            ).with((activity, processed) -> null);
        });
    }

    private Uni<Void> updateActivity(Long sellerId) {
        return Uni.createFrom().item(() -> {
            webSocketNotificationService.updateActivity(sellerId);
            return null;
        });
    }

    private Uni<Void> processMessage(String message, Session session, Long sellerId) {
        if (isHeartbeatMessage(message)) {
            return handleHeartbeat(session, sellerId);
        } else {
            return sendConfirmation(session, sellerId);
        }
    }

    private boolean isHeartbeatMessage(String message) {
        return message.contains("\"type\":\"PING\"") || message.contains("\"type\":\"HEARTBEAT\"");
    }

    private Uni<Void> handleHeartbeat(Session session, Long sellerId) {
        return Uni.createFrom().item(() -> {
            try {
                String pongMessage = "{\"type\":\"PONG\",\"message\":\"Heartbeat received\",\"sellerId\":" + sellerId + "}";
                session.getAsyncRemote().sendText(pongMessage);
                return null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Uni<Void> sendConfirmation(Session session, Long sellerId) {
        return Uni.createFrom().item(() -> {
            try {
                String confirmationMessage = "{\"type\":\"MESSAGE_RECEIVED\",\"message\":\"Message processed\",\"sellerId\":" + sellerId + "}";
                session.getAsyncRemote().sendText(confirmationMessage);
                return null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
