package org.sky.service;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.billing.TokenStatusResponse;
import org.sky.exception.InsufficientTokensException;
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

  private static final int DEFAULT_INITIAL_TOKENS = 100;

  private Uni<AdminTokens> findOrCreateAdminTokens(Long adminId) {
    return adminTokensRepository.findByAdminId(adminId)
        .chain(tokens -> {
          if (tokens == null) {
            AdminTokens newTokens = new AdminTokens();
            newTokens.adminId = adminId;
            newTokens.tokensAvailable = DEFAULT_INITIAL_TOKENS;
            newTokens.tokensUsed = 0;
            newTokens.tokensPurchased = 0;
            newTokens.lastResetDate = LocalDate.now();

            return adminTokensRepository.persist(newTokens);
          }
          return Uni.createFrom().item(tokens);
        });
  }

  @WithTransaction
  public Uni<Boolean> consumeTokens(Long adminId, String operationType, int tokensNeeded) {
    return findOrCreateAdminTokens(adminId)
        .chain(adminTokens -> {

          if (!adminTokens.hasEnoughTokens(tokensNeeded)) {
            return Uni.createFrom().failure(new InsufficientTokensException("Insufficient tokens"));
          }

          adminTokens.consumeTokens(tokensNeeded);

          return adminTokensRepository.persist(adminTokens)
              .chain(savedTokens -> {
                TokenUsageLog usageLog = new TokenUsageLog();
                usageLog.adminId = adminId;
                usageLog.operationType = operationType;
                usageLog.tokensConsumed = tokensNeeded;
                usageLog.operationDetails = String.format("{\"timestamp\": %d}", System.currentTimeMillis());

                return tokenUsageLogRepository.persist(usageLog)
                    .map(savedLog -> true);
              });
        });
  }

  @WithTransaction
  public Uni<TokenStatusResponse> getTokenStatus(Long adminId) {
    return findOrCreateAdminTokens(adminId)
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
    return findOrCreateAdminTokens(adminId)
        .chain(tokens -> {
          tokens.addTokens(tokensToAdd);
          return adminTokensRepository.persist(tokens)
              .replaceWithVoid();
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

}
