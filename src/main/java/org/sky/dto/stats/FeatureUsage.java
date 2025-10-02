package org.sky.dto.stats;

public record FeatureUsage(
    Double qrScannerUsage,
    Double paymentManagementUsage,
    Double analyticsUsage,
    Double notificationsUsage
) {}