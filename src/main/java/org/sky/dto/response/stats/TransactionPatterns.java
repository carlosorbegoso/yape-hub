package org.sky.dto.response.stats;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record TransactionPatterns(
    Double averageTransactionsPerDay,
    String mostActiveDay,
    String mostActiveHour,
    String transactionFrequency
) {
    public static TransactionPatterns empty() {
        return new TransactionPatterns(0.0, "", "", "");
    }
}
