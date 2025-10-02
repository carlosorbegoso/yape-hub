package org.sky.dto.stats;

public record TransactionPatterns(
    Double averageTransactionsPerDay,
    String mostActiveDay,
    String mostActiveHour,
    String transactionFrequency
) {}