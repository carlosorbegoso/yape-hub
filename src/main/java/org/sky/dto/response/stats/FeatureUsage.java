package org.sky.dto.response.stats;

public record FeatureUsage(
    Double qrScannerUsage,
    Double paymentManagementUsage,
    Double analyticsUsage,
    Double notificationsUsage
) {
    public static FeatureUsage empty() {
        return new FeatureUsage(0.0, 0.0, 0.0, 0.0);
    }
}