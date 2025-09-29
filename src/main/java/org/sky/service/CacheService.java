package org.sky.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.User;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class CacheService {

    // Cache para usuarios (5 minutos)
    private final Cache<String, User> userCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    // Cache para validación JWT (1 minuto)
    private final Cache<String, Boolean> jwtValidationCache = Caffeine.newBuilder()
            .maximumSize(5000)
            .expireAfterWrite(Duration.ofMinutes(1))
            .build();

    // Cache para tokens válidos (30 segundos)
    private final Cache<String, String> validTokenCache = Caffeine.newBuilder()
            .maximumSize(2000)
            .expireAfterWrite(Duration.ofSeconds(30))
            .build();

    public Uni<User> getCachedUser(String email, String role) {
        String key = email + ":" + role;
        
        return Uni.createFrom().completionStage(() -> {
            User cachedUser = userCache.getIfPresent(key);
            return CompletableFuture.completedFuture(cachedUser);
        });
    }

    public Uni<Void> cacheUser(String email, String role, User user) {
        String key = email + ":" + role;
        
        return Uni.createFrom().completionStage(() -> {
            userCache.put(key, user);
            return CompletableFuture.completedFuture(null);
        });
    }

    public Uni<Boolean> getCachedJwtValidation(String token) {
        return Uni.createFrom().completionStage(() -> {
            Boolean isValid = jwtValidationCache.getIfPresent(token);
            return CompletableFuture.completedFuture(isValid);
        });
    }

    public Uni<Void> cacheJwtValidation(String token, Boolean isValid) {
        return Uni.createFrom().completionStage(() -> {
            jwtValidationCache.put(token, isValid);
            return CompletableFuture.completedFuture(null);
        });
    }

    public Uni<String> getCachedValidToken(String token) {
        return Uni.createFrom().completionStage(() -> {
            String cachedToken = validTokenCache.getIfPresent(token);
            return CompletableFuture.completedFuture(cachedToken);
        });
    }

    public Uni<Void> cacheValidToken(String token, String userId) {
        return Uni.createFrom().completionStage(() -> {
            validTokenCache.put(token, userId);
            return CompletableFuture.completedFuture(null);
        });
    }

    public Uni<Void> invalidateUserCache(String email, String role) {
        String key = email + ":" + role;
        
        return Uni.createFrom().completionStage(() -> {
            userCache.invalidate(key);
            return CompletableFuture.completedFuture(null);
        });
    }

    public Uni<Void> invalidateAllCaches() {
        return Uni.createFrom().completionStage(() -> {
            userCache.invalidateAll();
            jwtValidationCache.invalidateAll();
            validTokenCache.invalidateAll();
            return CompletableFuture.completedFuture(null);
        });
    }

    // Métricas del cache
    public Uni<String> getCacheStats() {
        return Uni.createFrom().completionStage(() -> {
            String stats = String.format(
                "User Cache: %d entries, JWT Cache: %d entries, Token Cache: %d entries",
                userCache.estimatedSize(),
                jwtValidationCache.estimatedSize(),
                validTokenCache.estimatedSize()
            );
            return CompletableFuture.completedFuture(stats);
        });
    }
}
