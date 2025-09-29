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
    WebSocketTokenExtractor tokenExtractor;

    @Inject
    SecurityService securityService;

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
                .chain(v -> sendWelcomeMessage(session, sellerId));
    }

    public Uni<Void> sendWelcomeMessage(Session session, Long sellerId) {
        return Uni.createFrom().item(() -> {
            try {
                String welcomeMessage = createWelcomeMessage(sellerId);
                session.getAsyncRemote().sendText(welcomeMessage);
                log.info("⚡ Welcome message sent to seller " + sellerId);
                return null;
            } catch (Exception e) {
                log.error("❌ Error sending welcome message to seller " + sellerId, e);
                throw new RuntimeException(e);
            }
        });
    }

    private String createWelcomeMessage(Long sellerId) {
        return String.format("{\"type\":\"CONNECTED\",\"message\":\"WebSocket connection established\",\"sellerId\":%d,\"timestamp\":%d}", 
                           sellerId, System.currentTimeMillis());
    }

    public Uni<Void> sendErrorAndClose(Session session, String errorMessage) {
        return Uni.createFrom().item(() -> {
            try {
                if (session.isOpen()) {
                    String errorResponse = createErrorMessage(errorMessage);
                    session.getAsyncRemote().sendText(errorResponse, result -> {
                        if (result.getException() != null) {
                            log.error("Error sending error message: " + result.getException().getMessage());
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

    private String createErrorMessage(String errorMessage) {
        return String.format("{\"type\":\"ERROR\",\"message\":\"%s\",\"timestamp\":%d}", 
                           errorMessage, System.currentTimeMillis());
    }

    public Uni<Void> handleConnection(Session session, String sellerIdParam) {
        log.info("🔌 Handling WebSocket connection for seller: " + sellerIdParam);
        
        return validateSellerId(sellerIdParam)
                .chain(sellerId -> configureAndAuthenticate(session, sellerId))
                .onFailure().recoverWithUni(throwable -> sendErrorAndClose(session, throwable.getMessage()));
    }

    public Uni<Long> validateSellerId(String sellerIdParam) {
        return Uni.createFrom().item(() -> {
            try {
                Long sellerId = Long.parseLong(sellerIdParam);
                if (sellerId <= 0) {
                    throw new IllegalArgumentException("Invalid seller ID: " + sellerId);
                }
                return sellerId;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid seller ID format: " + sellerIdParam);
            }
        });
    }

    private Uni<Void> configureAndAuthenticate(Session session, Long sellerId) {
        configureSessionProperties(session, sellerId);
        
        return tokenExtractor.extractTokenFromSession(session)
                .chain(token -> {
                    if (token == null) {
                        throw new SecurityException("Authentication token required");
                    }
                    
                    String authorization = "Bearer " + token;
                    return securityService.validateSellerAuthorization(authorization, sellerId)
                            .chain(userId -> {
                                log.info("✅ Authentication successful for seller " + sellerId + ", userId: " + userId);
                                return registerSessionAndSendWelcome(sellerId, session);
                            });
                });
    }

    private void configureSessionProperties(Session session, Long sellerId) {
        session.setMaxTextMessageBufferSize(maxTextMessageBufferSize);
        session.setMaxIdleTimeout(maxIdleTimeout);
        log.info("🔌 Session configured for seller " + sellerId);
    }

}
