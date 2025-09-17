package org.sky.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.jboss.logging.Logger;
import org.sky.service.WebSocketNotificationService;
import org.sky.util.JwtUtil;
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
    JwtUtil jwtUtil;

    private static final Logger log = Logger.getLogger(PaymentWebSocketController.class);

    @OnOpen
    public void onOpen(Session session, @PathParam("sellerId") String sellerIdParam) {
        try {
            Long sellerId = Long.parseLong(sellerIdParam);
            
            // Verificar autenticación JWT
            String token = extractTokenFromSession(session);
            if (token == null) {
                sendErrorAndClose(session, "Token de autenticación requerido");
                return;
            }
            
            // Validar token JWT
            try {
                Long userId = jwtUtil.getUserIdFromToken(token);
                if (userId == null) {
                    sendErrorAndClose(session, "Token de autenticación inválido");
                    return;
                }
                
                // Extraer sellerId del token
                Long tokenSellerId = jwtUtil.getSellerIdFromToken(token);
                if (tokenSellerId == null) {
                    sendErrorAndClose(session, "Token no válido - falta sellerId");
                    return;
                }
                
                // Validar que el sellerId del token coincide con el sellerId de la URL
                if (!tokenSellerId.equals(sellerId)) {
                    sendErrorAndClose(session, "Token no válido para este vendedor");
                    return;
                }
                
                // Registrar la sesión usando el servicio
                webSocketNotificationService.registerSession(sellerId, session);
                
                // Enviar mensaje de confirmación usando el servicio WebSocket
                String welcomeMessage = "{\"type\":\"CONNECTED\",\"message\":\"Conexión WebSocket establecida\",\"sellerId\":" + sellerId + "}";
                webSocketNotificationService.sendNotification(sellerId, welcomeMessage);
                
            } catch (Exception jwtException) {
                sendErrorAndClose(session, "Token de autenticación inválido");
                return;
            }
            
        } catch (Exception e) {
            try {
                session.close();
            } catch (Exception closeException) {
                // Ignorar error al cerrar
            }
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("sellerId") String sellerIdParam) {
        try {
            Long sellerId = Long.parseLong(sellerIdParam);
            webSocketNotificationService.unregisterSession(sellerId);
        } catch (Exception e) {
            // Ignorar error
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable, @PathParam("sellerId") String sellerIdParam) {
        try {
            Long sellerId = Long.parseLong(sellerIdParam);
            webSocketNotificationService.unregisterSession(sellerId);
        } catch (Exception e) {
            // Ignorar error
        }
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("sellerId") String sellerIdParam) {
        try {
            Long sellerId = Long.parseLong(sellerIdParam);
            
            // Respuesta de confirmación usando contexto asíncrono
            vertx.runOnContext(v -> {
                try {
                    session.getBasicRemote().sendText("{\"type\":\"MESSAGE_RECEIVED\",\"message\":\"Mensaje procesado\",\"sellerId\":" + sellerId + "}");
                } catch (Exception e) {
                    // Ignorar error
                }
            });
            
        } catch (Exception e) {
            // Ignorar error
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
