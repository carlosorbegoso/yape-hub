package org.sky.service;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.billing.TokenStatusResponse;
import org.sky.model.AdminTokens;
import org.sky.model.TokenUsageLog;
import org.sky.repository.AdminTokensRepository;
import org.sky.repository.TokenUsageLogRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class TokenService {

    @Inject
    AdminTokensRepository adminTokensRepository;

    @Inject
    TokenUsageLogRepository tokenUsageLogRepository;

    @WithTransaction
    public Uni<Boolean> consumeTokens(Long adminId, String operationType, int tokensNeeded) {
        Log.info("🪙 TokenService.consumeTokens() - AdminId: " + adminId + ", Operación: " + operationType + ", Tokens: " + tokensNeeded);
        
        return adminTokensRepository.findOrCreateByAdminId(adminId)
                .chain(adminTokens -> {
                    if (adminTokens == null) {
                        Log.warn("❌ Admin " + adminId + " no tiene registro de tokens");
                        return Uni.createFrom().failure(new RuntimeException("Admin no encontrado"));
                    }
                    
                    if (!adminTokens.hasEnoughTokens(tokensNeeded)) {
                        Log.warn("❌ Tokens insuficientes para admin " + adminId + ". Disponibles: " + adminTokens.tokensAvailable + ", Necesarios: " + tokensNeeded);
                        return Uni.createFrom().failure(new InsufficientTokensException("Tokens insuficientes"));
                    }
                    
                    // Consumir tokens
                    adminTokens.consumeTokens(tokensNeeded);
                    
                    return adminTokensRepository.persist(adminTokens)
                            .chain(savedTokens -> {
                                // Registrar uso en log
                                TokenUsageLog usageLog = new TokenUsageLog();
                                usageLog.adminId = adminId;
                                usageLog.operationType = operationType;
                                usageLog.tokensConsumed = tokensNeeded;
                                usageLog.operationDetails = String.format("{\"timestamp\": %d}", System.currentTimeMillis());
                                
                                return tokenUsageLogRepository.persist(usageLog)
                                        .map(savedLog -> {
                                            Log.info("✅ Tokens consumidos exitosamente para admin " + adminId);
                                            return true;
                                        });
                            });
                });
    }

    @WithTransaction
    public Uni<TokenStatusResponse> getTokenStatus(Long adminId) {
        Log.info("🪙 TokenService.getTokenStatus() - AdminId: " + adminId);
        
        return adminTokensRepository.findOrCreateByAdminId(adminId)
                .map(tokens -> {
                    if (tokens == null) {
                        return new TokenStatusResponse(0, 0, 0, 0, 0.0);
                    }
                    
                    int daysUntilReset = calculateDaysUntilReset(tokens.lastResetDate);
                    double usagePercentage = calculateUsagePercentage(tokens.tokensUsed, tokens.tokensPurchased);
                    
                    return new TokenStatusResponse(
                        tokens.tokensAvailable,
                        tokens.tokensUsed,
                        tokens.tokensPurchased,
                        daysUntilReset,
                        usagePercentage
                    );
                });
    }

    @WithTransaction
    public Uni<Void> addTokens(Long adminId, int tokensToAdd) {
        Log.info("🪙 TokenService.addTokens() - AdminId: " + adminId + ", Tokens: " + tokensToAdd);
        
        return adminTokensRepository.findOrCreateByAdminId(adminId)
                .chain(tokens -> {
                    tokens.addTokens(tokensToAdd);
                    return adminTokensRepository.persist(tokens)
                            .replaceWithVoid();
                });
    }

    @WithTransaction
    public Uni<Void> resetMonthlyTokens(Long adminId, int newTokens) {
        Log.info("🪙 TokenService.resetMonthlyTokens() - AdminId: " + adminId + ", NewTokens: " + newTokens);
        
        return adminTokensRepository.findOrCreateByAdminId(adminId)
                .chain(tokens -> {
                    tokens.resetMonthlyTokens(newTokens);
                    return adminTokensRepository.persist(tokens)
                            .replaceWithVoid();
                });
    }

    @WithTransaction
    public Uni<Boolean> checkTokenLimits(Long adminId, int tokensNeeded) {
        Log.info("🪙 TokenService.checkTokenLimits() - AdminId: " + adminId + ", TokensNeeded: " + tokensNeeded);
        
        return adminTokensRepository.findOrCreateByAdminId(adminId)
                .map(tokens -> {
                    if (tokens == null) {
                        return false;
                    }
                    return tokens.hasEnoughTokens(tokensNeeded);
                });
    }

    private int calculateDaysUntilReset(LocalDate lastResetDate) {
        LocalDate nextReset = lastResetDate.plusMonths(1);
        LocalDate today = LocalDate.now();
        return (int) ChronoUnit.DAYS.between(today, nextReset);
    }

    private double calculateUsagePercentage(int tokensUsed, int tokensPurchased) {
        if (tokensPurchased == 0) {
            return 0.0;
        }
        return Math.min(100.0, (double) tokensUsed / tokensPurchased * 100);
    }

    // Excepción personalizada para tokens insuficientes
    public static class InsufficientTokensException extends RuntimeException {
        public InsufficientTokensException(String message) {
            super(message);
        }
    }
}
