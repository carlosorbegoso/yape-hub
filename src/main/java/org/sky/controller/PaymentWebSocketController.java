package org.sky.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.jboss.logging.Logger;
import org.sky.service.WebSocketNotificationService;
import io.vertx.core.Vertx;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ws/payments/{sellerId}")
@ApplicationScoped
public class PaymentWebSocketController {

    @Inject
    WebSocketNotificationService webSocketNotificationService;

    @Inject
    Vertx vertx;

    private static final Logger log = Logger.getLogger(PaymentWebSocketController.class);

    @OnOpen
    public void onOpen(Session session, @PathParam("sellerId") String sellerIdParam) {
        try {
            Long sellerId = Long.parseLong(sellerIdParam);
            log.info("🔗 WebSocket conectado para vendedor: " + sellerId);
            
            // Registrar la sesión usando el servicio
            webSocketNotificationService.registerSession(sellerId, session);
            
            // Enviar mensaje de confirmación usando el servicio WebSocket
            String welcomeMessage = "{\"type\":\"CONNECTED\",\"message\":\"Conexión WebSocket establecida\",\"sellerId\":" + sellerId + "}";
            webSocketNotificationService.sendNotification(sellerId, welcomeMessage);
            log.info("✅ WebSocket registrado para vendedor " + sellerId);
            
        } catch (Exception e) {
            log.error("❌ Error al abrir WebSocket para vendedor " + sellerIdParam + ": " + e.getMessage());
            try {
                session.close();
            } catch (Exception closeException) {
                log.error("❌ Error al cerrar sesión: " + closeException.getMessage());
            }
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("sellerId") String sellerIdParam) {
        try {
            Long sellerId = Long.parseLong(sellerIdParam);
            log.info("🔌 WebSocket desconectado para vendedor: " + sellerId);
            
            // Desregistrar la sesión usando el servicio
            webSocketNotificationService.unregisterSession(sellerId);
            
            log.info("✅ WebSocket desregistrado para vendedor " + sellerId);
            
        } catch (Exception e) {
            log.error("❌ Error al cerrar WebSocket para vendedor " + sellerIdParam + ": " + e.getMessage());
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable, @PathParam("sellerId") String sellerIdParam) {
        log.error("❌ Error en WebSocket para vendedor " + sellerIdParam + ": " + throwable.getMessage());
        
        try {
            Long sellerId = Long.parseLong(sellerIdParam);
            webSocketNotificationService.unregisterSession(sellerId);
        } catch (Exception e) {
            log.error("❌ Error al limpiar sesión después del error: " + e.getMessage());
        }
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("sellerId") String sellerIdParam) {
        try {
            Long sellerId = Long.parseLong(sellerIdParam);
            log.info("📨 Mensaje recibido de vendedor " + sellerId + ": " + message);
            
            // Aquí podrías procesar mensajes del cliente si es necesario
            // Por ejemplo, confirmaciones de pago, heartbeats, etc.
            
            // Respuesta de confirmación usando contexto asíncrono
            vertx.runOnContext(v -> {
                try {
                    session.getBasicRemote().sendText("{\"type\":\"MESSAGE_RECEIVED\",\"message\":\"Mensaje procesado\",\"sellerId\":" + sellerId + "}");
                } catch (Exception e) {
                    log.error("❌ Error enviando respuesta: " + e.getMessage());
                }
            });
            
        } catch (Exception e) {
            log.error("❌ Error al procesar mensaje de vendedor " + sellerIdParam + ": " + e.getMessage());
        }
    }

}
