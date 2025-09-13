package org.sky.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SellerDashboardResponse(
    SellerInfo sellerInfo,
    SummaryInfo summary,
    List<RecentTransaction> recentTransactions
) {
    public record SellerInfo(
        Long id,
        String name,
        String branchName,
        Boolean isActive
    ) {}
    
    public record SummaryInfo(
        int totalTransactions,
        BigDecimal totalAmount,
        int pendingTransactions,
        int todayTransactions,
        BigDecimal todayAmount
    ) {}
    
    public record RecentTransaction(
        Long id,
        BigDecimal amount,
        LocalDateTime timestamp,
        Boolean isProcessed
    ) {}
}
