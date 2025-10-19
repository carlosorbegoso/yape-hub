package org.sky.service.websocket;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.Session;
import jakarta.websocket.SendResult;
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

    public void registerSession(Long sellerId, Session session) {
        webSocketNotificationService.registerSession(sellerId, session);
    }

    public void unregisterSession(Long sellerId) {
        webSocketNotificationService.unregisterSession(sellerId);
    }

    public Uni<Void> registerSessionAndSendWelcome(Long sellerId, Session session) {
        registerSession(sellerId, session);
        return sendWelcomeMessage(session, sellerId);
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
            sendErrorMessage(session, errorMessage);
            closeSessionSafely(session);
            return null;
        });
    }

    private void sendErrorMessage(Session session, String errorMessage) {
        if (!session.isOpen()) {
            return;
        }

        try {
            String errorResponse = createErrorMessage(errorMessage);
            session.getAsyncRemote().sendText(errorResponse, this::handleSendResult);
        } catch (Exception e) {
            log.error("Error sending error message: " + e.getMessage());
        }
    }

    private void handleSendResult(SendResult result) {
        if (result.getException() != null) {
            log.error("Error sending error message: " + result.getException().getMessage());
        }
    }

    private void closeSessionSafely(Session session) {
        if (!session.isOpen()) {
            return;
        }

        try {
            session.close();
        } catch (Exception e) {
            log.debug("Session already closed: " + e.getMessage());
        }
    }

    private String createErrorMessage(String errorMessage) {
        return String.format("{\"type\":\"ERROR\",\"message\":\"%s\",\"timestamp\":%d}", 
                           errorMessage, System.currentTimeMillis());
    }

  public Uni<Void> handleConnection(Session session, String sellerIdParam) {
    log.info("🔌 Handling WebSocket connection for seller: " + sellerIdParam);
    log.info("🔍 Session Query Parameters: " + session.getRequestParameterMap());
    log.info("🔍 Session Query String: " + session.getQueryString());

    return validateSellerId(sellerIdParam)
        .chain(sellerId -> {
          log.info("✅ Validated seller ID: " + sellerId);
          return configureAndAuthenticate(session, sellerId);
        })
        .onFailure().invoke(throwable -> {
          log.error("❌ WebSocket connection failed: " + throwable.getMessage(), throwable);
        });
  }

    public Uni<Long> validateSellerId(String sellerIdParam) {
        return Uni.createFrom().item(() -> {
            try {
                long sellerId = Long.parseLong(sellerIdParam);
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
            log.error("❌ No token found in WebSocket session for seller " + sellerId);
            return Uni.createFrom().failure(new SecurityException("No authentication token"));
          }

          // Add more detailed logging for token validation
          log.info("🔍 Attempting to validate token for seller " + sellerId);

          String authorization = "Bearer " + token;
          return securityService.validateSellerAuthorization(authorization, sellerId)
              .chain(userId -> {
                log.info("✅ Authentication successful for seller " + sellerId + ", userId: " + userId);
                return registerSessionAndSendWelcome(sellerId, session);
              })
              .onFailure().invoke(throwable -> {
                log.error("❌ Token validation failed for seller " + sellerId + ": " + throwable.getMessage());
              });
        });
  }

    private void configureSessionProperties(Session session, Long sellerId) {
        session.setMaxTextMessageBufferSize(maxTextMessageBufferSize);
        session.setMaxIdleTimeout(maxIdleTimeout);
        log.info("🔌 Session configured for seller " + sellerId);
    }

}
