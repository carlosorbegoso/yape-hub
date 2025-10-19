package org.sky.service.cache;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.sky.model.UserEntityEntity;
import org.sky.repository.UserRepository;

import java.util.concurrent.ConcurrentHashMap;

/**
 * SERVICIO DE CACHÉ SIMPLE Y EFICIENTE
 * Usa solo APIs nativas sin dependencias externas - optimizado para recursos limitados
 */
@ApplicationScoped
public class CacheService {

    private static final Logger log = Logger.getLogger(CacheService.class);
    
    // Cache simple con ConcurrentHashMap - ultra eficiente
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> accessTimes = new ConcurrentHashMap<>();
    
    private static final long CACHE_TTL = 300_000; // 5 minutos
    private static final int MAX_CACHE_SIZE = 200; // Máximo 200 entradas
    private static final long CLEANUP_THRESHOLD = 180_000; // 3 minutos de cleanup
    
    @Inject
    UserRepository userRepository;
    
    // ==================================================================================
    // CACHE DE USUARIOS OPTIMIZADO
    // ==================================================================================
    
    public Uni<UserEntityEntity> getUserByEmailCached(String email) {
        String cacheKey = "user:email:" + email;
        
        return Uni.createFrom().item(() -> {
            long currentTime = System.currentTimeMillis();
            quickCleanupIfNeeded();
            
            CacheEntry cached = cache.get(cacheKey);
            if (cached != null && (currentTime - cached.timestamp) < CACHE_TTL) {
                accessTimes.put(cacheKey, currentTime);
                log.debug("🚀 Cache HIT for user email: " + email);
                return cached.user;
            }
            
            log.debug("💾 Cache MISS for user email: " + email);
            return null; // Cache miss, will be handled reactively
        })
        .chain(cachedUser -> {
            if (cachedUser != null && (cachedUser.role == null || cachedUser.email == null || cachedUser.email.trim().isEmpty())) {
                    log.warn("⚠️ Corrupted user found in cache: ID=" + cachedUser.id + " email=" + email + " role=" + cachedUser.role);
                    // Force database reload instead of corrupted cache
                    cachedUser = null;
                }


          if (cachedUser != null) {
                return Uni.createFrom().item(cachedUser);
            }
            // Load from DB reactively
            return userRepository.findByEmail(email)
                .map(user -> {
                    if (user != null && user.role != null && user.email != null && !user.email.trim().isEmpty()) { // Only cache valid users
                        cacheUser(cacheKey, user);
                    }
                    return user;
                });
        });
    }

    private void cacheUser(String key, UserEntityEntity user) {
        // LRU simple - eliminar entrada más antigua si alcanzamos límite
        if (cache.size() >= MAX_CACHE_SIZE) {
            String oldestKey = accessTimes.entrySet().stream()
                .min(ConcurrentHashMap.Entry.comparingByValue())
                .map(ConcurrentHashMap.Entry::getKey)
                .orElse(null);
            
            if (oldestKey != null) {
                cache.remove(oldestKey);
                accessTimes.remove(oldestKey);
            }
        }
        
        cache.put(key, new CacheEntry(user));
        accessTimes.put(key, System.currentTimeMillis());
        log.debug("💾 User cached: " + key);
    }
    
    // Cleanup rápido y eficiente
    private void quickCleanupIfNeeded() {
        long currentTime = System.currentTimeMillis();
        
        // Solo cleanup si han pasado suficientes tiempo
        if (cache.isEmpty()) return;
        
        Long firstAccess = accessTimes.values().stream().min(Long::compareTo).orElse(currentTime);
        if (currentTime - firstAccess < CLEANUP_THRESHOLD) return;
        
        // Cleanup concurrente - thread safe
        cache.entrySet().removeIf(entry -> {
            String key = entry.getKey();
            CacheEntry cached = entry.getValue();
            Long accessTime = accessTimes.get(key);
            
            if (accessTime == null || cached == null) {
                accessTimes.remove(key);
                return true;
            }
            
            boolean expired = (currentTime - cached.timestamp) >= CACHE_TTL;
            if (expired) {
                accessTimes.remove(key);
            }
            
            return expired;
        });
        
        log.debug("🧹 Cleaned expired cache entries");
    }

    public Uni<UserEntityEntity> getCachedUser(String email, String role) {
        return getUserByEmailCached(email)
                .chain(cachedUser -> {
                    // Validate cached user has complete data
                    if (cachedUser != null && (cachedUser.role == null || cachedUser.email == null || cachedUser.email.trim().isEmpty())) {
                        log.warn("⚠️ Corrupted user found in cache: ID=" + cachedUser.id + " email=" + email + " role=" + cachedUser.role);
                        // Return null to force database reload instead of corrupted cache
                        return Uni.createFrom().item((UserEntityEntity) null);
                    }
                    return Uni.createFrom().item(cachedUser);
                });
    }


  public Uni<Void> cacheUser(String email, String role, UserEntityEntity user) {
        // Solo usar email como clave, ignorar role duplicado
        String cacheKey = "user:email:" + email;
        return Uni.createFrom().item(() -> {
            cacheUser(cacheKey, user);
            return null;
        });
    }

    private static class CacheEntry {
        final UserEntityEntity user;
        final long timestamp;
        
        CacheEntry(UserEntityEntity user) {
            this.user = user;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
