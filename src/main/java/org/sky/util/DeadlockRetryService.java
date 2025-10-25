package org.sky.util;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Servicio para manejar reintentos automáticos en caso de deadlocks de PostgreSQL
 */
@ApplicationScoped
public class DeadlockRetryService {
    
    private static final Logger log = Logger.getLogger(DeadlockRetryService.class);
    
    // Configuración de reintentos
    private static final int MAX_RETRIES = 3;
    private static final Duration INITIAL_DELAY = Duration.ofMillis(100);
    private static final double BACKOFF_MULTIPLIER = 2.0;

    public <T> Uni<T> executeWithRetry(Supplier<Uni<T>> operation, String operationName) {
        return executeWithRetry(operation, operationName, MAX_RETRIES, INITIAL_DELAY);
    }
    

    public <T> Uni<T> executeWithRetry(Supplier<Uni<T>> operation, String operationName, int maxRetries, Duration initialDelay) {
        return executeWithRetryInternal(operation, operationName, maxRetries, initialDelay, 0);
    }
    
    /**
     * Implementación interna recursiva para los reintentos
     */
    private <T> Uni<T> executeWithRetryInternal(Supplier<Uni<T>> operation, String operationName, 
                                               int maxRetries, Duration currentDelay, int attempt) {
        
        return operation.get()
            .onFailure().recoverWithUni(throwable -> {
                if (isDeadlockError(throwable) && attempt < maxRetries) {
                    log.warnf("🔄 Deadlock detectado en %s (intento %d/%d). Reintentando en %dms...", 
                             operationName, attempt + 1, maxRetries + 1, currentDelay.toMillis());
                    
                    // Calcular el siguiente delay con backoff exponencial
                    Duration nextDelay = Duration.ofMillis(
                        (long) (currentDelay.toMillis() * BACKOFF_MULTIPLIER)
                    );
                    
                    // Reintentar con el siguiente delay
                    return Uni.createFrom().item(throwable)
                        .onItem().delayIt().by(currentDelay)
                        .chain(t -> executeWithRetryInternal(operation, operationName, maxRetries, nextDelay, attempt + 1));
                } else {
                    // No es deadlock o se agotaron los reintentos
                    log.errorf("❌ Error en %s después de %d intentos: %s", 
                              operationName, attempt + 1, throwable.getMessage());
                    return Uni.createFrom().failure(throwable);
                }
            });
    }
    
    /**
     * Verifica si el error es un deadlock de PostgreSQL
     */
    private boolean isDeadlockError(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        
        String message = throwable.getMessage();
        if (message == null) {
            return false;
        }
        
        // Detectar deadlock de PostgreSQL
        return message.toLowerCase().contains("deadlock detected") ||
               message.contains("40P01") || // Código de error específico de deadlock
               message.toLowerCase().contains("lock timeout") ||
               message.toLowerCase().contains("could not serialize access");
    }
    
    /**
     * Método de conveniencia para operaciones que no devuelven valor
     */
    public Uni<Void> executeWithRetryVoid(Supplier<Uni<Void>> operation, String operationName) {
        return executeWithRetry(operation, operationName)
            .replaceWithVoid();
    }
    
    /**
     * Método de conveniencia para operaciones que devuelven Uni<Void>
     */
    public Uni<Void> executeWithRetryVoid(Supplier<Uni<Void>> operation, String operationName, 
                                        int maxRetries, Duration initialDelay) {
        return executeWithRetry(operation, operationName, maxRetries, initialDelay)
            .replaceWithVoid();
    }
}
