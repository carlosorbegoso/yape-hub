package org.sky.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio para manejar notificaciones WebSocket
 * Esta interfaz permite que los servicios envíen notificaciones sin conocer la implementación WebSocket
 */
@ApplicationScoped
public class WebSocketNotificationService {
    
    private static final Logger log = Logger.getLogger(WebSocketNotificationService.class);
    
    // Mapa para almacenar las sesiones WebSocket por sellerId
    private final Map<Long, jakarta.websocket.Session> webSocketSessions = new ConcurrentHashMap<>();
    
    /**
     * Registra una sesión WebSocket para un vendedor
     */
    public void registerSession(Long sellerId, jakarta.websocket.Session session) {
        webSocketSessions.put(sellerId, session);
        log.info("🔗 Sesión WebSocket registrada para vendedor " + sellerId);
    }
    
    /**
     * Desregistra una sesión WebSocket para un vendedor
     */
    public void unregisterSession(Long sellerId) {
        webSocketSessions.remove(sellerId);
        log.info("🔌 Sesión WebSocket desregistrada para vendedor " + sellerId);
    }
    
    /**
     * Envía una notificación a un vendedor específico
     */
    public void sendNotification(Long sellerId, String message) {
        jakarta.websocket.Session session = webSocketSessions.get(sellerId);
        if (session != null && session.isOpen()) {
            try {
                // Usar sendText de forma asíncrona para evitar problemas de hilos
                session.getAsyncRemote().sendText(message);
                log.info("📱 Notificación enviada a vendedor " + sellerId + " via WebSocket");
            } catch (Exception e) {
                log.error("❌ Error al enviar notificación a vendedor " + sellerId + ": " + e.getMessage());
                // Remover sesión si hay error
                webSocketSessions.remove(sellerId);
            }
        } else {
            log.warn("⚠️ No hay sesión WebSocket activa para vendedor " + sellerId);
        }
    }
    
    /**
     * Verifica si hay una conexión WebSocket activa para un vendedor
     */
    public boolean hasActiveConnection(Long sellerId) {
        jakarta.websocket.Session session = webSocketSessions.get(sellerId);
        return session != null && session.isOpen();
    }
    
    /**
     * Obtiene el número de conexiones WebSocket activas
     */
    public int getActiveConnectionsCount() {
        return (int) webSocketSessions.values().stream()
                .filter(session -> session != null && session.isOpen())
                .count();
    }
    
    /**
     * Obtiene la lista de vendedores con conexiones activas
     */
    public java.util.List<Long> getActiveSellerIds() {
        return webSocketSessions.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().isOpen())
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Verifica si un vendedor está conectado
     */
    public boolean isSellerConnected(Long sellerId) {
        jakarta.websocket.Session session = webSocketSessions.get(sellerId);
        return session != null && session.isOpen();
    }
    
    /**
     * Obtiene el número de vendedores conectados
     */
    public int getConnectedSellersCount() {
        return webSocketSessions.size();
    }
    
    /**
     * Obtiene todos los IDs de vendedores conectados
     */
    public java.util.Set<Long> getConnectedSellerIds() {
        return webSocketSessions.keySet();
    }
    
    /**
     * Verifica si hay vendedores conectados para un admin específico
     */
    public boolean hasConnectedSellersForAdmin(Long adminId) {
        // Esta implementación básica solo verifica si hay conexiones
        // En una implementación más completa, necesitaríamos acceso al SellerRepository
        return !webSocketSessions.isEmpty();
    }
}
