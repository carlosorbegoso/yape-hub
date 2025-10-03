package org.sky.dto.response.stats;

public record TransactionPatterns(
    Double averageTransactionsPerDay,
    String mostActiveDay,
    String mostActiveHour,
    String transactionFrequency
) {}