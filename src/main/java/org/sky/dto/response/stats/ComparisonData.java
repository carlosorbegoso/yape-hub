package org.sky.dto.response.stats;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record ComparisonData(
    Double salesChange,
    Long transactionChange,
    Double percentageChange
) {
    public static ComparisonData empty() {
        return new ComparisonData(0.0, 0L, 0.0);
    }
}
