package org.sky.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.ApiResponse;
import org.sky.dto.dashboard.AdminDashboardResponse;
import org.sky.dto.dashboard.SellerDashboardResponse;
import org.sky.model.Branch;
import org.sky.model.Seller;
import org.sky.model.Transaction;
import org.sky.repository.BranchRepository;
import org.sky.repository.SellerRepository;
import org.sky.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class DashboardService {
    
    @Inject
    TransactionRepository transactionRepository;
    
    @Inject
    SellerRepository sellerRepository;
    
    @Inject
    BranchRepository branchRepository;
    
    public Uni<ApiResponse<AdminDashboardResponse>> getAdminDashboard(Long adminId, String period, Long branchId) {
        Uni<List<Transaction>> transactionsUni = transactionRepository.findByAdminId(adminId);
        Uni<List<Seller>> sellersUni = sellerRepository.findByAdminId(adminId);
        Uni<List<Branch>> branchesUni = branchRepository.findByAdminId(adminId);
        
        return Uni.combine().all().unis(transactionsUni, sellersUni, branchesUni)
                .with((allTransactions, sellers, branches) -> {
                    // Filter by branch if specified
                    if (branchId != null) {
                        allTransactions = allTransactions.stream()
                                .filter(txn -> txn.branch.id.equals(branchId))
                                .collect(Collectors.toList());
                    }
                    
                    // Filter by period
                    LocalDateTime startDate = getStartDateForPeriod(period);
                    List<Transaction> filteredTransactions = allTransactions.stream()
                            .filter(txn -> txn.timestamp.isAfter(startDate))
                            .collect(Collectors.toList());
                    
                    // Calculate summary
                    int totalTransactions = filteredTransactions.size();
                    BigDecimal totalAmount = filteredTransactions.stream()
                            .map(txn -> txn.amount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    int pendingTransactions = (int) filteredTransactions.stream()
                            .filter(txn -> !txn.isProcessed)
                            .count();
                    
                    int activeSellers = (int) sellers.stream()
                            .filter(seller -> seller.isActive)
                            .count();
                    
                    int totalBranches = branches.size();
                    
                    AdminDashboardResponse.SummaryInfo summary = new AdminDashboardResponse.SummaryInfo(
                            totalTransactions, totalAmount, pendingTransactions, activeSellers, totalBranches
                    );
                    
                    // Generate daily stats (mock data)
                    List<AdminDashboardResponse.DailyStats> dailyStats = generateDailyStats(filteredTransactions);
                    
                    // Generate top sellers (mock data)
                    List<AdminDashboardResponse.TopSeller> topSellers = generateTopSellers(sellers);
                    
                    // Generate branch stats (mock data)
                    List<AdminDashboardResponse.BranchStats> branchStats = generateBranchStats(branches);
                    
                    AdminDashboardResponse response = new AdminDashboardResponse(summary, dailyStats, topSellers, branchStats);
                    
                    return ApiResponse.success("Dashboard obtenido exitosamente", response);
                });
    }
    
    public Uni<ApiResponse<SellerDashboardResponse>> getSellerDashboard(Long sellerId, String period) {
        Uni<Seller> sellerUni = sellerRepository.findById(sellerId);
        Uni<List<Transaction>> transactionsUni = transactionRepository.findBySellerId(sellerId);
        
        return Uni.combine().all().unis(sellerUni, transactionsUni)
                .with((seller, allTransactions) -> {
                    if (seller == null) {
                        return ApiResponse.<SellerDashboardResponse>error("Vendedor no encontrado");
                    }
                    
                    // Get seller info
                    SellerDashboardResponse.SellerInfo sellerInfo = new SellerDashboardResponse.SellerInfo(
                            seller.id, seller.sellerName, seller.branch.name, seller.isActive
                    );
                    
                    // Filter by period
                    LocalDateTime startDate = getStartDateForPeriod(period);
                    List<Transaction> filteredTransactions = allTransactions.stream()
                            .filter(txn -> txn.timestamp.isAfter(startDate))
                            .collect(Collectors.toList());
                    
                    // Calculate summary
                    int totalTransactions = filteredTransactions.size();
                    BigDecimal totalAmount = filteredTransactions.stream()
                            .map(txn -> txn.amount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    int pendingTransactions = (int) filteredTransactions.stream()
                            .filter(txn -> !txn.isProcessed)
                            .count();
                    
                    // Today's transactions
                    LocalDateTime todayStart = LocalDate.now().atStartOfDay();
                    LocalDateTime todayEnd = LocalDate.now().atTime(23, 59, 59);
                    
                    List<Transaction> todayTransactions = allTransactions.stream()
                            .filter(txn -> txn.timestamp.isAfter(todayStart) && txn.timestamp.isBefore(todayEnd))
                            .collect(Collectors.toList());
                    
                    int todayTransactionCount = todayTransactions.size();
                    BigDecimal todayAmount = todayTransactions.stream()
                            .map(txn -> txn.amount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    SellerDashboardResponse.SummaryInfo summary = new SellerDashboardResponse.SummaryInfo(
                            totalTransactions, totalAmount, pendingTransactions, todayTransactionCount, todayAmount
                    );
                    
                    // Recent transactions (last 5)
                    List<Transaction> recentTransactions = allTransactions.stream()
                            .sorted((t1, t2) -> t2.timestamp.compareTo(t1.timestamp))
                            .limit(5)
                            .collect(Collectors.toList());
                    
                    List<SellerDashboardResponse.RecentTransaction> recentTransactionResponses = recentTransactions.stream()
                            .map(txn -> new SellerDashboardResponse.RecentTransaction(
                                    txn.id, txn.amount, txn.timestamp, txn.isProcessed))
                            .collect(Collectors.toList());
                    
                    SellerDashboardResponse response = new SellerDashboardResponse(sellerInfo, summary, recentTransactionResponses);
                    
                    return ApiResponse.success("Dashboard obtenido exitosamente", response);
                });
    }
    
    private LocalDateTime getStartDateForPeriod(String period) {
        LocalDate today = LocalDate.now();
        switch (period) {
            case "today":
                return today.atStartOfDay();
            case "week":
                return today.minusWeeks(1).atStartOfDay();
            case "month":
                return today.minusMonths(1).atStartOfDay();
            case "year":
                return today.minusYears(1).atStartOfDay();
            default:
                return today.minusMonths(1).atStartOfDay();
        }
    }
    
    private List<AdminDashboardResponse.DailyStats> generateDailyStats(List<Transaction> transactions) {
        // Mock implementation - in real app, you'd group by date
        List<AdminDashboardResponse.DailyStats> stats = new ArrayList<>();
        stats.add(new AdminDashboardResponse.DailyStats("2024-01-15", 45, new BigDecimal("1250.75")));
        stats.add(new AdminDashboardResponse.DailyStats("2024-01-14", 38, new BigDecimal("980.50")));
        stats.add(new AdminDashboardResponse.DailyStats("2024-01-13", 52, new BigDecimal("1450.25")));
        return stats;
    }
    
    private List<AdminDashboardResponse.TopSeller> generateTopSellers(List<Seller> sellers) {
        // Mock implementation - in real app, you'd calculate from actual transaction data
        return sellers.stream()
                .limit(5)
                .map(seller -> new AdminDashboardResponse.TopSeller(
                        seller.id, seller.sellerName, seller.totalPayments, seller.totalAmount))
                .collect(Collectors.toList());
    }
    
    private List<AdminDashboardResponse.BranchStats> generateBranchStats(List<Branch> branches) {
        // Mock implementation - in real app, you'd calculate from actual transaction data
        return branches.stream()
                .map(branch -> new AdminDashboardResponse.BranchStats(
                        branch.id, branch.name, 85, new BigDecimal("2150.25")))
                .collect(Collectors.toList());
    }
}
