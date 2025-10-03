package org.sky.dto.response.stats;

public record FeatureUsage(
    Double qrScannerUsage,
    Double paymentManagementUsage,
    Double analyticsUsage,
    Double notificationsUsage
) {}