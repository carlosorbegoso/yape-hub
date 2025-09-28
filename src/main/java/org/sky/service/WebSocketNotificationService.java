package org.sky.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import io.vertx.core.Vertx;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Servicio para manejar notificaciones WebSocket
 * Esta interfaz permite que los servicios envíen notificaciones sin conocer la implementación WebSocket
 */
@ApplicationScoped
public class WebSocketNotificationService {
    
    @Inject
    Vertx vertx;
    
    private static final Logger log = Logger.getLogger(WebSocketNotificationService.class);
    
    // Mapa para almacenar las sesiones WebSocket por sellerId
    private final Map<Long, jakarta.websocket.Session> webSocketSessions = new ConcurrentHashMap<>();
    
    // Mapa para almacenar el último tiempo de actividad por sellerId
    private final Map<Long, AtomicLong> lastActivityTime = new ConcurrentHashMap<>();
    
    // ID del timer de limpieza
    private Long cleanupTimerId;
    
    // Configuración
    private static final long CLEANUP_INTERVAL_MS = 30000; // 30 segundos
    private static final long SESSION_TIMEOUT_MS = 300000; // 5 minutos
    
    /**
     * Registra una sesión WebSocket para un vendedor
     */
    public void registerSession(Long sellerId, jakarta.websocket.Session session) {
        webSocketSessions.put(sellerId, session);
        lastActivityTime.put(sellerId, new AtomicLong(System.currentTimeMillis()));
        
        // Iniciar timer de limpieza si no está activo
        if (cleanupTimerId == null) {
            startCleanupTimer();
        }
        
        log.info("🔗 Sesión WebSocket registrada para vendedor " + sellerId + " - Total sesiones: " + webSocketSessions.size());
        log.info("🔗 Estado de sesión: " + (session != null ? "No nula" : "Nula") + ", Abierta: " + (session != null ? session.isOpen() : "N/A"));
    }
    
    /**
     * Desregistra una sesión WebSocket para un vendedor
     */
    public void unregisterSession(Long sellerId) {
        webSocketSessions.remove(sellerId);
        lastActivityTime.remove(sellerId);
        log.info("🔌 Sesión WebSocket desregistrada para vendedor " + sellerId + " - Total sesiones: " + webSocketSessions.size());
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
                
                // Actualizar tiempo de actividad
                AtomicLong lastActivity = lastActivityTime.get(sellerId);
                if (lastActivity != null) {
                    lastActivity.set(System.currentTimeMillis());
                }
                
                log.info("📱 Notificación enviada a vendedor " + sellerId + " via WebSocket");
            } catch (Exception e) {
                log.error("❌ Error al enviar notificación a vendedor " + sellerId + ": " + e.getMessage());
                // Remover sesión si hay error
                unregisterSession(sellerId);
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
        boolean isActive = session != null && session.isOpen();
        log.info("🔍 hasActiveConnection(" + sellerId + ") - Sesión encontrada: " + (session != null) + ", Abierta: " + (session != null ? session.isOpen() : "N/A") + ", Resultado: " + isActive);
        return isActive;
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
        log.info("🔍 getConnectedSellerIds() - Total sesiones en mapa: " + webSocketSessions.size());
        java.util.Set<Long> activeSessions = webSocketSessions.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().isOpen())
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
        log.info("🔍 getConnectedSellerIds() - Sesiones activas: " + activeSessions.size() + " - IDs: " + activeSessions);
        return activeSessions;
    }
    
    /**
     * Verifica si hay vendedores conectados para un admin específico
     */
    public boolean hasConnectedSellersForAdmin(Long adminId) {
        // Esta implementación básica solo verifica si hay conexiones
        // En una implementación más completa, necesitaríamos acceso al SellerRepository
        return !webSocketSessions.isEmpty();
    }
    
    /**
     * Inicia el timer de limpieza automática de sesiones inactivas
     */
    private void startCleanupTimer() {
        if (cleanupTimerId != null) {
            return; // Ya está activo
        }
        
        cleanupTimerId = vertx.setPeriodic(CLEANUP_INTERVAL_MS, timerId -> {
            cleanupInactiveSessions();
        });
        
        log.info("🧹 Timer de limpieza de sesiones WebSocket iniciado (intervalo: " + CLEANUP_INTERVAL_MS + "ms)");
    }
    
    /**
     * Limpia sesiones WebSocket inactivas
     */
    private void cleanupInactiveSessions() {
        long currentTime = System.currentTimeMillis();
        java.util.List<Long> inactiveSellers = new java.util.ArrayList<>();
        
        for (Map.Entry<Long, AtomicLong> entry : lastActivityTime.entrySet()) {
            Long sellerId = entry.getKey();
            AtomicLong lastActivity = entry.getValue();
            
            if (lastActivity != null) {
                long timeSinceLastActivity = currentTime - lastActivity.get();
                if (timeSinceLastActivity > SESSION_TIMEOUT_MS) {
                    inactiveSellers.add(sellerId);
                }
            }
        }
        
        // Remover sesiones inactivas
        for (Long sellerId : inactiveSellers) {
            jakarta.websocket.Session session = webSocketSessions.get(sellerId);
            if (session != null && !session.isOpen()) {
                log.info("🧹 Limpiando sesión inactiva para vendedor " + sellerId);
                unregisterSession(sellerId);
            }
        }
        
        if (!inactiveSellers.isEmpty()) {
            log.info("🧹 Limpieza completada: " + inactiveSellers.size() + " sesiones inactivas removidas");
        }
    }
    
    /**
     * Actualiza el tiempo de actividad para un vendedor
     */
    public void updateActivity(Long sellerId) {
        AtomicLong lastActivity = lastActivityTime.get(sellerId);
        if (lastActivity != null) {
            lastActivity.set(System.currentTimeMillis());
        }
    }
    
    /**
     * Obtiene estadísticas de las sesiones WebSocket
     */
    public Map<String, Object> getSessionStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalSessions", webSocketSessions.size());
        stats.put("activeSessions", getActiveConnectionsCount());
        stats.put("cleanupTimerActive", cleanupTimerId != null);
        stats.put("sessionTimeoutMs", SESSION_TIMEOUT_MS);
        stats.put("cleanupIntervalMs", CLEANUP_INTERVAL_MS);
        return stats;
    }
}
