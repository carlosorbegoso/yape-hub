package org.sky.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.jboss.logging.Logger;
import org.sky.service.websocket.WebSocketMessageHandler;
import org.sky.service.websocket.WebSocketSessionManager;
import org.sky.service.security.SecurityService;

@ServerEndpoint("/ws/payments/{sellerId}")
@ApplicationScoped
public class PaymentWebSocketController {

    // ONLY used services - removed SecurityService and WebSocketTokenExtractor duplication
    @Inject
    WebSocketMessageHandler messageHandler;

    @Inject
    WebSocketSessionManager sessionManager;

    @Inject
    SecurityService securityService;

    private static final Logger log = Logger.getLogger(PaymentWebSocketController.class);

    @OnOpen
    public void onOpen(Session session, @PathParam("sellerId") String sellerIdParam) {
        sessionManager.handleConnection(session, sellerIdParam)
                .subscribe().with(
                    success -> {},
                    error -> log.error("Error in onOpen for seller " + sellerIdParam + ": " + error.getMessage())
                );
    }

    @OnClose
    public void onClose(Session session, @PathParam("sellerId") String sellerIdParam) {
        try {
            Long sellerId = Long.parseLong(sellerIdParam);
            sessionManager.unregisterSession(sellerId);
        } catch (Exception e) {
            log.error("Error in onClose for seller " + sellerIdParam + ": " + e.getMessage());
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable, @PathParam("sellerId") String sellerIdParam) {
        log.error("WebSocket error for seller " + sellerIdParam + ": " + throwable.getMessage());
        try {
            Long sellerId = Long.parseLong(sellerIdParam);
            sessionManager.unregisterSession(sellerId);
        } catch (Exception e) {
            log.error("Error in onError for seller " + sellerIdParam + ": " + e.getMessage());
        }
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("sellerId") String sellerIdParam) {
        sessionManager.validateSellerId(sellerIdParam)
                .chain(sellerId -> messageHandler.handleMessage(message, session, sellerIdParam))
                .subscribe().with(
                    success -> {},
                    error -> log.error("Error processing message from seller " + sellerIdParam + ": " + error.getMessage())
                );
    }
}
