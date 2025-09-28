package org.sky.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.jboss.logging.Logger;
import org.sky.service.WebSocketNotificationService;
import org.sky.util.jwt.JwtExtractor;
import org.sky.util.jwt.JwtValidator;
import io.vertx.core.Vertx;

import java.util.Map;
import java.util.List;

@ServerEndpoint("/ws/payments/{sellerId}")
@ApplicationScoped
public class PaymentWebSocketController {

    @Inject
    WebSocketNotificationService webSocketNotificationService;

    @Inject
    Vertx vertx;
    
    @Inject
    JwtExtractor jwtExtractor;
    
    @Inject
    JwtValidator jwtValidator;

    private static final Logger log = Logger.getLogger(PaymentWebSocketController.class);

    @OnOpen
    public void onOpen(Session session, @PathParam("sellerId") String sellerIdParam) {
        log.info("🔌 Intento de conexión WebSocket para vendedor: " + sellerIdParam);
        try {
            Long sellerId = Long.parseLong(sellerIdParam);
            
            // Verificar autenticación JWT
            String token = extractTokenFromSession(session);
            if (token == null) {
                log.warn("❌ Conexión WebSocket rechazada - Token de autenticación requerido para vendedor: " + sellerId);
                sendErrorAndClose(session, "Token de autenticación requerido");
                return;
            }
            
            // Validar token JWT
            try {
                Long userId = jwtExtractor.extractUserId(jwtValidator.parseToken(token).await().indefinitely()).await().indefinitely();
                if (userId == null) {
                    log.warn("❌ Conexión WebSocket rechazada - Token de autenticación inválido para vendedor: " + sellerId);
                    sendErrorAndClose(session, "Token de autenticación inválido");
                    return;
                }
                
                // Extraer sellerId del token
                Long tokenSellerId = jwtExtractor.extractSellerId(jwtValidator.parseToken(token).await().indefinitely()).await().indefinitely();
                if (tokenSellerId == null) {
                    log.warn("❌ Conexión WebSocket rechazada - Token no válido (falta sellerId) para vendedor: " + sellerId);
                    sendErrorAndClose(session, "Token no válido - falta sellerId");
                    return;
                }
                
                // Validar que el sellerId del token coincide con el sellerId de la URL
                if (!tokenSellerId.equals(sellerId)) {
                    log.warn("❌ Conexión WebSocket rechazada - Token no válido para este vendedor. Esperado: " + sellerId + ", Token: " + tokenSellerId);
                    sendErrorAndClose(session, "Token no válido para este vendedor");
                    return;
                }
                
                // Registrar la sesión usando el servicio
                webSocketNotificationService.registerSession(sellerId, session);
                log.info("✅ Conexión WebSocket establecida exitosamente para vendedor: " + sellerId);
                
                // Enviar mensaje de confirmación usando el servicio WebSocket
                String welcomeMessage = "{\"type\":\"CONNECTED\",\"message\":\"Conexión WebSocket establecida\",\"sellerId\":" + sellerId + "}";
                webSocketNotificationService.sendNotification(sellerId, welcomeMessage);
                
            } catch (Exception jwtException) {
                log.error("❌ Error validando JWT para vendedor " + sellerId + ": " + jwtException.getMessage());
                sendErrorAndClose(session, "Token de autenticación inválido");
                return;
            }
            
        } catch (Exception e) {
            log.error("❌ Error en onOpen para vendedor " + sellerIdParam + ": " + e.getMessage());
            try {
                session.close();
            } catch (Exception closeException) {
                log.error("❌ Error cerrando sesión después de error en onOpen: " + closeException.getMessage());
            }
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("sellerId") String sellerIdParam) {
        log.info("🔌 Desconexión WebSocket para vendedor: " + sellerIdParam);
        try {
            Long sellerId = Long.parseLong(sellerIdParam);
            webSocketNotificationService.unregisterSession(sellerId);
            log.info("✅ Sesión WebSocket desregistrada exitosamente para vendedor: " + sellerId);
        } catch (Exception e) {
            log.error("❌ Error en onClose para vendedor " + sellerIdParam + ": " + e.getMessage());
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable, @PathParam("sellerId") String sellerIdParam) {
        log.error("❌ Error WebSocket para vendedor " + sellerIdParam + ": " + throwable.getMessage());
        try {
            Long sellerId = Long.parseLong(sellerIdParam);
            webSocketNotificationService.unregisterSession(sellerId);
            log.info("✅ Sesión WebSocket desregistrada después de error para vendedor: " + sellerId);
        } catch (Exception e) {
            log.error("❌ Error en onError para vendedor " + sellerIdParam + ": " + e.getMessage());
        }
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("sellerId") String sellerIdParam) {
        try {
            Long sellerId = Long.parseLong(sellerIdParam);
            log.info("📨 Mensaje recibido de vendedor " + sellerId + ": " + message);
            
            // Actualizar tiempo de actividad
            webSocketNotificationService.updateActivity(sellerId);
            
            // Procesar mensajes de heartbeat
            if (message.contains("\"type\":\"PING\"") || message.contains("\"type\":\"HEARTBEAT\"")) {
                vertx.runOnContext(v -> {
                    try {
                        String pongMessage = "{\"type\":\"PONG\",\"message\":\"Heartbeat recibido\",\"sellerId\":" + sellerId + "}";
                        session.getAsyncRemote().sendText(pongMessage);
                        log.info("💓 Heartbeat respondido para vendedor " + sellerId);
                    } catch (Exception e) {
                        log.error("❌ Error enviando PONG a vendedor " + sellerId + ": " + e.getMessage());
                    }
                });
                return;
            }
            
            // Respuesta de confirmación para otros mensajes
            vertx.runOnContext(v -> {
                try {
                    session.getAsyncRemote().sendText("{\"type\":\"MESSAGE_RECEIVED\",\"message\":\"Mensaje procesado\",\"sellerId\":" + sellerId + "}");
                } catch (Exception e) {
                    log.error("❌ Error enviando confirmación a vendedor " + sellerId + ": " + e.getMessage());
                }
            });
            
        } catch (Exception e) {
            log.error("❌ Error procesando mensaje de vendedor " + sellerIdParam + ": " + e.getMessage());
        }
    }
    
    
    /**
     * Extrae el token JWT de la sesión WebSocket
     */
    private String extractTokenFromSession(Session session) {
        try {
            // Intentar obtener el token de los query parameters
            String queryString = session.getQueryString();
            
            if (queryString != null && !queryString.isEmpty()) {
                // Buscar el parámetro token en la query string
                String[] params = queryString.split("&");
                for (String param : params) {
                    if (param.startsWith("token=")) {
                        return param.substring(6); // Remove "token="
                    }
                }
            }
            
            // Intentar obtener el token de los headers
            Map<String, List<String>> headers = session.getRequestParameterMap();
            
            if (headers != null && headers.containsKey("authorization")) {
                List<String> authHeaders = headers.get("authorization");
                if (authHeaders != null && !authHeaders.isEmpty()) {
                    String authHeader = authHeaders.get(0);
                    if (authHeader.startsWith("Bearer ")) {
                        return authHeader.substring(7);
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Envía un mensaje de error y cierra la conexión
     */
    private void sendErrorAndClose(Session session, String errorMessage) {
        try {
            String errorResponse = "{\"type\":\"ERROR\",\"message\":\"" + errorMessage + "\"}";
            session.getBasicRemote().sendText(errorResponse);
            session.close();
        } catch (Exception e) {
            try {
                session.close();
            } catch (Exception closeException) {
                // Ignorar error
            }
        }
    }
    
    /**
     * Envía un mensaje de error y cierra la conexión de forma asíncrona
     */
    private void sendErrorAndCloseAsync(Session session, String errorMessage) {
        vertx.runOnContext(v -> {
            try {
                String errorResponse = "{\"type\":\"ERROR\",\"message\":\"" + errorMessage + "\"}";
                session.getAsyncRemote().sendText(errorResponse);
                session.close();
            } catch (Exception e) {
                try {
                    session.close();
                } catch (Exception closeException) {
                    // Ignorar error
                }
            }
        });
    }

}
