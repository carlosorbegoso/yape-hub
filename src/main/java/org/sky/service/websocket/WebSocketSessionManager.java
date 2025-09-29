package org.sky.service.websocket;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.Session;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.sky.service.security.SecurityService;

@ApplicationScoped
public class WebSocketSessionManager {

    @Inject
    WebSocketNotificationService webSocketNotificationService;

    @Inject
    SecurityService securityService;

    @Inject
    WebSocketTokenExtractor tokenExtractor;

    @ConfigProperty(name = "websocket.session.max-text-message-buffer-size", defaultValue = "4096")
    int maxTextMessageBufferSize;

    @ConfigProperty(name = "websocket.session.max-idle-timeout", defaultValue = "60000")
    int maxIdleTimeout;

    private static final Logger log = Logger.getLogger(WebSocketSessionManager.class);

    public Uni<Void> registerSession(Long sellerId, Session session) {
        return Uni.createFrom().item(() -> {
            webSocketNotificationService.registerSession(sellerId, session);
            return null;
        });
    }

    public Uni<Void> unregisterSession(Long sellerId) {
        return Uni.createFrom().item(() -> {
            webSocketNotificationService.unregisterSession(sellerId);
            return null;
        });
    }

    public Uni<Void> registerSessionAndSendWelcome(Long sellerId, Session session) {
        return registerSession(sellerId, session)
                .chain(v -> {
                    try {
                        // Optimized welcome message for real-time performance
                        String welcomeMessage = "{\"type\":\"CONNECTED\",\"message\":\"WebSocket connection established\",\"sellerId\":" + sellerId + ",\"timestamp\":" + System.currentTimeMillis() + "}";
                        
                        // Send message immediately for real-time performance
                        session.getAsyncRemote().sendText(welcomeMessage);
                        
                        log.info("⚡ Real-time welcome message sent to seller " + sellerId);
                        return Uni.createFrom().voidItem();
                    } catch (Exception e) {
                        log.error("❌ Error sending welcome message to seller " + sellerId, e);
                        return Uni.createFrom().failure(e);
                    }
                });
    }

    public Uni<Void> sendErrorAndClose(Session session, String errorMessage) {
        return Uni.createFrom().item(() -> {
            try {
                if (session.isOpen()) {
                    String errorResponse = "{\"type\":\"ERROR\",\"message\":\"" + errorMessage + "\",\"timestamp\":" + System.currentTimeMillis() + "}";
                    session.getAsyncRemote().sendText(errorResponse, result -> {
                        if (result.getException() != null) {
                            log.error("Error sending error message to WebSocket: " + result.getException().getMessage());
                        }
                        try {
                            if (session.isOpen()) {
                                session.close();
                            }
                        } catch (Exception closeException) {
                            // Session already closed
                        }
                    });
                } else {
                    session.close();
                }
                return null;
            } catch (Exception e) {
                log.error("Error in sendErrorAndClose: " + e.getMessage());
                try {
                    if (session.isOpen()) {
                        session.close();
                    }
                } catch (Exception closeException) {
                    // Session already closed
                }
                return null;
            }
        });
    }

    public Uni<Void> handleConnection(Session session, String sellerIdParam) {
        log.info("🔌 Handling WebSocket connection for seller: " + sellerIdParam);
        return Uni.createFrom().item(() -> {
            try {
                Long sellerId = Long.parseLong(sellerIdParam);
                
                        // Validate seller ID range
                        if (sellerId <= 0) {
                            log.warn("❌ Invalid seller ID: " + sellerId);
                            return null;
                        }
                
                // Set session properties for robustness
                session.setMaxTextMessageBufferSize(maxTextMessageBufferSize);
                session.setMaxIdleTimeout(maxIdleTimeout);
                log.info("🔌 Session configured for seller " + sellerId);
                
                return sellerId;
                    } catch (NumberFormatException e) {
                        log.error("❌ Invalid seller ID format: " + sellerIdParam);
                        return null;
                    }
        })
        .chain(sellerId -> {
            if (sellerId == null) {
                return sendErrorAndClose(session, "Invalid seller ID");
            }
            
            return tokenExtractor.extractTokenFromSession(session)
                            .chain(token -> {
                                log.info("🔐 Extracted token for seller " + sellerId + ": " + (token != null ? "present" : "null"));
                                if (token == null) {
                                    return sendErrorAndClose(session, "Authentication token required");
                                }
                        
                        String authorization = "Bearer " + token;
                        log.info("🔐 Validating authorization for seller " + sellerId);
                        return securityService.validateSellerAuthorization(authorization, sellerId)
                                .chain(userId -> {
                                    log.info("✅ Authentication successful for seller " + sellerId + ", userId: " + userId);
                                    return registerSessionAndSendWelcome(sellerId, session);
                                })
                                .onFailure().recoverWithUni(throwable -> {
                                    log.error("❌ Authentication failed for seller " + sellerId + ": " + throwable.getMessage());
                                    return sendErrorAndClose(session, "Authentication failed: " + throwable.getMessage());
                                });
                    });
        });
    }

}
