package org.sky.service.websocket;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.Session;
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

    public Uni<Void> sendWelcomeMessage(Long sellerId, Session session) {
        return Uni.createFrom().item(() -> {
            try {
                String welcomeMessage = "{\"type\":\"CONNECTED\",\"message\":\"WebSocket connection established\",\"sellerId\":" + sellerId + "}";
                session.getBasicRemote().sendText(welcomeMessage);
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to send welcome message", e);
            }
        });
    }

    public Uni<Void> registerSessionAndSendWelcome(Long sellerId, Session session) {
        return Uni.combine().all().unis(
                registerSession(sellerId, session),
                sendWelcomeMessage(sellerId, session)
        ).with((registered, welcome) -> null);
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
        return Uni.createFrom().item(() -> {
            try {
                Long sellerId = Long.parseLong(sellerIdParam);
                
                        // Validate seller ID range
                        if (sellerId <= 0) {
                            return null;
                        }
                
                // Set session properties for robustness
                session.setMaxTextMessageBufferSize(8192); // 8KB
                session.setMaxIdleTimeout(30000); // 30 seconds
                
                return sellerId;
                    } catch (NumberFormatException e) {
                        return null;
                    }
        })
        .chain(sellerId -> {
            if (sellerId == null) {
                return sendErrorAndClose(session, "Invalid seller ID");
            }
            
            return tokenExtractor.extractTokenFromSession(session)
                            .chain(token -> {
                                if (token == null) {
                                    return sendErrorAndClose(session, "Authentication token required");
                                }
                        
                        String authorization = "Bearer " + token;
                        return securityService.validateSellerAuthorization(authorization, sellerId)
                                .chain(userId -> {
                                    return registerSessionAndSendWelcome(sellerId, session);
                                })
                                .onFailure().recoverWithUni(throwable -> {
                                    return sendErrorAndClose(session, "Authentication failed: " + throwable.getMessage());
                                });
                    });
        });
    }

}
