package org.sky.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.jboss.logging.Logger;
import org.sky.service.websocket.WebSocketMessageHandler;
import org.sky.service.websocket.WebSocketSessionManager;

@ServerEndpoint("/ws/payments/{sellerId}")
@ApplicationScoped
public class PaymentWebSocketController {

  @Inject
  WebSocketMessageHandler messageHandler;

  @Inject
  WebSocketSessionManager sessionManager;

  private static final Logger log = Logger.getLogger(PaymentWebSocketController.class);

  @OnOpen
  public void onOpen(Session session, @PathParam("sellerId") String sellerIdParam) {
    log.info("🔌 Attempting to open WebSocket connection");
    log.info("🔍 Seller ID Parameter: " + sellerIdParam);
    log.info("🔍 Full Session Details: " +
        "Query String: " + session.getQueryString() +
        ", Request Parameters: " + session.getRequestParameterMap());

    try {
      Long sellerId = Long.parseLong(sellerIdParam);

      log.info("🔐 Initiating WebSocket connection for seller: " + sellerId);

      sessionManager.handleConnection(session, sellerIdParam)
          .subscribe().with(
              success -> log.info("✅ WebSocket connection established for seller " + sellerId),
              error -> {
                log.error("❌ WebSocket connection failed for seller " + sellerId + ": " + error.getMessage());
                // Log the full error stack trace
                if (error instanceof Throwable) {
                  ((Throwable) error).printStackTrace();
                }
              }
          );
    } catch (NumberFormatException e) {
      log.error("❌ Invalid seller ID format: " + sellerIdParam);
    }
  }

  @OnClose
  public void onClose(Session session, @PathParam("sellerId") String sellerIdParam) {
    try {
      Long sellerId = Long.parseLong(sellerIdParam);
      log.info("🔌 Closing WebSocket connection for seller: " + sellerId);
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
      log.warn("❌ WebSocket error handling for seller: " + sellerId);
      sessionManager.unregisterSession(sellerId);
    } catch (Exception e) {
      log.error("Error in onError handler: " + e.getMessage());
    }
  }

  @OnMessage
  public void onMessage(String message, Session session, @PathParam("sellerId") String sellerIdParam) {
    log.info("📩 Received message from seller: " + sellerIdParam);

    sessionManager.validateSellerId(sellerIdParam)
        .chain(sellerId -> {
          log.info("✅ Validated seller ID: " + sellerId);
          return messageHandler.handleMessage(message, session, sellerIdParam);
        })
        .subscribe().with(
            success -> log.debug("✅ Message processed successfully"),
            error -> log.error("❌ Error processing message from seller " + sellerIdParam + ": " + error.getMessage())
        );
  }
}