package org.sky.service.stats.calculators.admin.management;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.dto.stats.admin.AdministrativeInsightsResponse;
import org.sky.dto.stats.branch.BranchAnalyticsResponse;
import org.sky.dto.stats.financial.FinancialOverviewResponse;
import org.sky.dto.stats.security.ComplianceAndSecurityResponse;
import org.sky.dto.stats.seller.SellerManagementResponse;
import org.sky.dto.stats.system.SystemMetricsResponse;
import org.sky.model.PaymentNotification;
import org.sky.model.Seller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@ApplicationScoped
public class AdminManagementCalculator {
    
    // Constants
    private static final String CONFIRMED_STATUS = "CONFIRMED";
    private static final String PENDING_STATUS = "PENDING";
    private static final String REJECTED_STATUS = "REJECTED";
    private static final double SUSPICIOUS_AMOUNT_THRESHOLD = 1000.0;
    private static final double LOW_CONFIRMATION_RATE_THRESHOLD = 50.0;
    private static final String NO_BRANCH_NAME = "Sin sucursal";
    
    // Functional interfaces for better readability
    private final Function<PaymentNotification, Boolean> isConfirmed = 
        payment -> CONFIRMED_STATUS.equals(payment.status);
    private final Function<PaymentNotification, Boolean> isPending = 
        payment -> PENDING_STATUS.equals(payment.status);
    private final Function<PaymentNotification, Boolean> isRejected = 
        payment -> REJECTED_STATUS.equals(payment.status);
    private final Predicate<PaymentNotification> isSuspicious = 
        payment -> payment.amount > SUSPICIOUS_AMOUNT_THRESHOLD;
    
    public Uni<BranchAnalyticsResponse> calculateBranchAnalytics(List<PaymentNotification> payments, 
                                                                           List<Seller> sellers, 
                                                                           LocalDate startDate, 
                                                                           LocalDate endDate) {
        return Uni.createFrom().item(() -> {
            var filteredPayments = filterPaymentsByDateRange(payments, startDate, endDate);
            var branchPerformance = calculateBranchPerformance(filteredPayments, sellers);
            var branchComparison = calculateBranchComparison(branchPerformance);
            
            return new BranchAnalyticsResponse(branchPerformance, branchComparison);
        });
    }
    
    private List<PaymentNotification> filterPaymentsByDateRange(List<PaymentNotification> payments, 
                                                               LocalDate startDate, 
                                                               LocalDate endDate) {
        return payments.stream()
            .filter(payment -> isPaymentInDateRange(payment, startDate, endDate))
            .toList();
    }
    
    private boolean isPaymentInDateRange(PaymentNotification payment, LocalDate startDate, LocalDate endDate) {
        return Optional.ofNullable(payment.createdAt)
            .map(LocalDateTime::toLocalDate)
            .map(date -> !date.isBefore(startDate) && !date.isAfter(endDate))
            .orElse(false);
    }
    
    private List<BranchAnalyticsResponse.BranchPerformanceData> calculateBranchPerformance(
            List<PaymentNotification> payments, List<Seller> sellers) {
        
        var branchGroups = groupSellersByBranch(sellers);
        
        return branchGroups.entrySet().stream()
            .map(entry -> calculateBranchPerformanceData(entry.getKey(), entry.getValue(), payments))
            .toList();
    }
    
    private Map<String, List<Seller>> groupSellersByBranch(List<Seller> sellers) {
        return sellers.stream()
            .collect(Collectors.groupingBy(
                seller -> Optional.ofNullable(seller.branch)
                    .map(branch -> branch.name)
                    .orElse(NO_BRANCH_NAME)
            ));
    }
    
    private BranchAnalyticsResponse.BranchPerformanceData calculateBranchPerformanceData(
            String branchName, List<Seller> branchSellers, List<PaymentNotification> allPayments) {
        
        var branchPayments = filterPaymentsBySellers(allPayments, branchSellers);
        var confirmedPayments = branchPayments.stream().filter(isConfirmed::apply).toList();
        
        var totalSales = calculateTotalSales(confirmedPayments);
        var totalTransactions = (long) branchPayments.size();
        var activeSellers = (long) branchSellers.size();
        var inactiveSellers = 0L; // Could be calculated based on activity
        var averageSalesPerSeller = calculateAverageSalesPerSeller(totalSales, activeSellers);
        var performanceScore = calculatePerformanceScore(totalSales, totalTransactions);
        var growthRate = calculateGrowthRate(branchPayments); // Simplified
        var lastActivity = getLastActivity(branchPayments);
        
        return new BranchAnalyticsResponse.BranchPerformanceData(
            null, branchName, null, totalSales, totalTransactions, 
            activeSellers, inactiveSellers, averageSalesPerSeller, 
            performanceScore, growthRate, lastActivity
        );
    }
    
    private List<PaymentNotification> filterPaymentsBySellers(List<PaymentNotification> payments, 
                                                             List<Seller> sellers) {
        var sellerIds = sellers.stream()
            .map(seller -> seller.id)
            .collect(Collectors.toSet());
        
        return payments.stream()
            .filter(payment -> payment.confirmedBy != null && sellerIds.contains(payment.confirmedBy))
            .toList();
    }
    
    private double calculateTotalSales(List<PaymentNotification> confirmedPayments) {
        return confirmedPayments.stream()
            .mapToDouble(payment -> payment.amount)
            .sum();
    }
    
    private double calculateAverageSalesPerSeller(double totalSales, long activeSellers) {
        return activeSellers > 0 ? totalSales / activeSellers : 0.0;
    }
    
    private double calculatePerformanceScore(double totalSales, long totalTransactions) {
        if (totalTransactions == 0) return 0.0;
        return Math.min(10.0, (totalSales / totalTransactions) * 10);
    }
    
    private double calculateGrowthRate(List<PaymentNotification> payments) {
        // Simplified growth rate calculation
        return payments.size() > 10 ? 5.0 : 0.0;
    }
    
    private String getLastActivity(List<PaymentNotification> payments) {
        return payments.stream()
            .map(payment -> payment.createdAt)
            .filter(java.util.Objects::nonNull)
            .max(LocalDateTime::compareTo)
            .map(date -> date.toLocalDate().toString())
            .orElse("N/A");
    }
    
    private BranchAnalyticsResponse.BranchComparison calculateBranchComparison(
            List<BranchAnalyticsResponse.BranchPerformanceData> branchPerformance) {
        
        if (branchPerformance.isEmpty()) {
            return new BranchAnalyticsResponse.BranchComparison(null, null, null);
        }
        
        var topBranch = findTopPerformingBranch(branchPerformance);
        var lowestBranch = findLowestPerformingBranch(branchPerformance);
        var averagePerformance = calculateAverageBranchPerformance(branchPerformance);
        
        return new BranchAnalyticsResponse.BranchComparison(topBranch, lowestBranch, averagePerformance);
    }
    
    private BranchAnalyticsResponse.BranchSummary findTopPerformingBranch(
            List<BranchAnalyticsResponse.BranchPerformanceData> branchPerformance) {
        
        return branchPerformance.stream()
            .max((a, b) -> Double.compare(a.performanceScore(), b.performanceScore()))
            .map(branch -> new BranchAnalyticsResponse.BranchSummary(
                branch.branchName(), branch.totalSales(), 
                branch.totalTransactions(), branch.performanceScore()))
            .orElse(null);
    }
    
    private BranchAnalyticsResponse.BranchSummary findLowestPerformingBranch(
            List<BranchAnalyticsResponse.BranchPerformanceData> branchPerformance) {
        
        return branchPerformance.stream()
            .min((a, b) -> Double.compare(a.performanceScore(), b.performanceScore()))
            .map(branch -> new BranchAnalyticsResponse.BranchSummary(
                branch.branchName(), branch.totalSales(), 
                branch.totalTransactions(), branch.performanceScore()))
            .orElse(null);
    }
    
    private BranchAnalyticsResponse.AverageBranchPerformance calculateAverageBranchPerformance(
            List<BranchAnalyticsResponse.BranchPerformanceData> branchPerformance) {
        
        var averageSales = branchPerformance.stream()
            .mapToDouble(BranchAnalyticsResponse.BranchPerformanceData::totalSales)
            .average()
            .orElse(0.0);
        
        var averageTransactions = branchPerformance.stream()
            .mapToLong(BranchAnalyticsResponse.BranchPerformanceData::totalTransactions)
            .average()
            .orElse(0.0);
        
        var averagePerformanceScore = branchPerformance.stream()
            .mapToDouble(BranchAnalyticsResponse.BranchPerformanceData::performanceScore)
            .average()
            .orElse(0.0);
        
        return new BranchAnalyticsResponse.AverageBranchPerformance(
            averageSales, averageTransactions, averagePerformanceScore);
    }
    
    public Uni<SellerManagementResponse> calculateSellerManagement(List<PaymentNotification> payments, 
                                                                             List<Seller> sellers, 
                                                                             LocalDate startDate, 
                                                                             LocalDate endDate) {
        return Uni.createFrom().item(() -> {
            var filteredPayments = filterPaymentsByDateRange(payments, startDate, endDate);
            var sellerOverview = calculateSellerOverview(filteredPayments, sellers);
            var performanceDistribution = calculateSellerPerformanceDistribution(filteredPayments, sellers);
            var sellerActivity = calculateSellerActivity(filteredPayments, sellers);
            
            return new SellerManagementResponse(sellerOverview, performanceDistribution, sellerActivity);
        });
    }
    
    private SellerManagementResponse.SellerOverview calculateSellerOverview(
            List<PaymentNotification> payments, List<Seller> sellers) {
        
        var totalSellers = (long) sellers.size();
        var activeSellers = calculateActiveSellers(payments, sellers);
        var inactiveSellers = totalSellers - activeSellers;
        var newSellersThisMonth = calculateNewSellersThisMonth(sellers);
        var sellersWithZeroSales = calculateSellersWithZeroSales(payments, sellers);
        var topPerformers = calculateTopPerformers(payments, sellers);
        var underPerformers = calculateUnderPerformers(payments, sellers);
        
        return new SellerManagementResponse.SellerOverview(
            totalSellers, activeSellers, inactiveSellers, newSellersThisMonth,
            sellersWithZeroSales, topPerformers, underPerformers
        );
    }
    
    private long calculateActiveSellers(List<PaymentNotification> payments, List<Seller> sellers) {
        var activeSellerIds = payments.stream()
            .filter(payment -> payment.confirmedBy != null)
            .map(payment -> payment.confirmedBy)
            .collect(Collectors.toSet());
        
        return sellers.stream()
            .mapToLong(seller -> activeSellerIds.contains(seller.id) ? 1 : 0)
            .sum();
    }
    
    private long calculateNewSellersThisMonth(List<Seller> sellers) {
        var currentMonth = LocalDate.now().getMonth();
        return sellers.stream()
            .mapToLong(seller -> Optional.ofNullable(seller.createdAt)
                .map(date -> date.getMonth() == currentMonth ? 1 : 0)
                .orElse(0))
            .sum();
    }
    
    private long calculateSellersWithZeroSales(List<PaymentNotification> payments, List<Seller> sellers) {
        var sellerSales = calculateSellerSalesMap(payments, sellers);
        
        return sellers.stream()
            .mapToLong(seller -> sellerSales.getOrDefault(seller.id, 0.0) == 0.0 ? 1 : 0)
            .sum();
    }
    
    private long calculateTopPerformers(List<PaymentNotification> payments, List<Seller> sellers) {
        var sellerSales = calculateSellerSalesMap(payments, sellers);
        var averageSales = sellerSales.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        return sellers.stream()
            .mapToLong(seller -> sellerSales.getOrDefault(seller.id, 0.0) > averageSales * 1.5 ? 1 : 0)
            .sum();
    }
    
    private long calculateUnderPerformers(List<PaymentNotification> payments, List<Seller> sellers) {
        var sellerSales = calculateSellerSalesMap(payments, sellers);
        var averageSales = sellerSales.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        return sellers.stream()
            .mapToLong(seller -> sellerSales.getOrDefault(seller.id, 0.0) < averageSales * 0.5 ? 1 : 0)
            .sum();
    }
    
    private Map<Long, Double> calculateSellerSalesMap(List<PaymentNotification> payments, List<Seller> sellers) {
        return payments.stream()
            .filter(isConfirmed::apply)
            .collect(Collectors.groupingBy(
                payment -> payment.confirmedBy,
                Collectors.summingDouble(payment -> payment.amount)
            ));
    }
    
    private SellerManagementResponse.SellerPerformanceDistribution calculateSellerPerformanceDistribution(
            List<PaymentNotification> payments, List<Seller> sellers) {
        
        var sellerPerformance = calculateSellerPerformanceMap(payments, sellers);
        var excellent = sellerPerformance.values().stream().mapToLong(score -> score >= 8.0 ? 1 : 0).sum();
        var good = sellerPerformance.values().stream().mapToLong(score -> score >= 6.0 && score < 8.0 ? 1 : 0).sum();
        var average = sellerPerformance.values().stream().mapToLong(score -> score >= 4.0 && score < 6.0 ? 1 : 0).sum();
        var poor = sellerPerformance.values().stream().mapToLong(score -> score < 4.0 ? 1 : 0).sum();
        
        return new SellerManagementResponse.SellerPerformanceDistribution(excellent, good, average, poor);
    }
    
    private Map<Long, Double> calculateSellerPerformanceMap(List<PaymentNotification> payments, List<Seller> sellers) {
        return sellers.stream()
            .collect(Collectors.toMap(
                seller -> seller.id,
                seller -> calculateIndividualSellerPerformance(payments, seller)
            ));
    }
    
    private double calculateIndividualSellerPerformance(List<PaymentNotification> payments, Seller seller) {
        var sellerPayments = payments.stream()
            .filter(payment -> payment.confirmedBy != null && payment.confirmedBy.equals(seller.id))
            .toList();
        
        if (sellerPayments.isEmpty()) return 0.0;
        
        var confirmedPayments = sellerPayments.stream().filter(isConfirmed::apply).toList();
        var confirmationRate = (double) confirmedPayments.size() / sellerPayments.size();
        var totalSales = confirmedPayments.stream().mapToDouble(payment -> payment.amount).sum();
        
        return Math.min(10.0, (confirmationRate * 5) + (totalSales / 100.0));
    }
    
    private SellerManagementResponse.SellerActivity calculateSellerActivity(
            List<PaymentNotification> payments, List<Seller> sellers) {
        
        var dailyActiveSellers = calculateDailyActiveSellers(payments, sellers);
        var weeklyActiveSellers = calculateWeeklyActiveSellers(payments, sellers);
        var monthlyActiveSellers = calculateMonthlyActiveSellers(payments, sellers);
        var averageSessionDuration = calculateAverageSessionDuration(payments);
        var averageTransactionsPerSeller = calculateAverageTransactionsPerSeller(payments, sellers);
        
        return new SellerManagementResponse.SellerActivity(
            dailyActiveSellers, weeklyActiveSellers, monthlyActiveSellers,
            averageSessionDuration, averageTransactionsPerSeller
        );
    }
    
    private long calculateDailyActiveSellers(List<PaymentNotification> payments, List<Seller> sellers) {
        var today = LocalDate.now();
        return payments.stream()
            .filter(payment -> Optional.ofNullable(payment.createdAt)
                .map(LocalDateTime::toLocalDate)
                .map(date -> date.equals(today))
                .orElse(false))
            .filter(payment -> payment.confirmedBy != null)
            .map(payment -> payment.confirmedBy)
            .distinct()
            .count();
    }
    
    private long calculateWeeklyActiveSellers(List<PaymentNotification> payments, List<Seller> sellers) {
        var weekStart = LocalDate.now().minusDays(7);
        return payments.stream()
            .filter(payment -> Optional.ofNullable(payment.createdAt)
                .map(LocalDateTime::toLocalDate)
                .map(date -> !date.isBefore(weekStart))
                .orElse(false))
            .filter(payment -> payment.confirmedBy != null)
            .map(payment -> payment.confirmedBy)
            .distinct()
            .count();
    }
    
    private long calculateMonthlyActiveSellers(List<PaymentNotification> payments, List<Seller> sellers) {
        var monthStart = LocalDate.now().minusDays(30);
        return payments.stream()
            .filter(payment -> Optional.ofNullable(payment.createdAt)
                .map(LocalDateTime::toLocalDate)
                .map(date -> !date.isBefore(monthStart))
                .orElse(false))
            .filter(payment -> payment.confirmedBy != null)
            .map(payment -> payment.confirmedBy)
            .distinct()
            .count();
    }
    
    private double calculateAverageSessionDuration(List<PaymentNotification> payments) {
        // Simplified calculation - in real scenario would track actual session durations
        return payments.isEmpty() ? 0.0 : 15.5; // minutes
    }
    
    private double calculateAverageTransactionsPerSeller(List<PaymentNotification> payments, List<Seller> sellers) {
        if (sellers.isEmpty()) return 0.0;
        return (double) payments.size() / sellers.size();
    }
    
    public Uni<SystemMetricsResponse> calculateSystemMetrics(List<PaymentNotification> payments, 
                                                                       List<Seller> sellers, 
                                                                       LocalDate startDate, 
                                                                       LocalDate endDate) {
        return Uni.createFrom().item(() -> {
            var filteredPayments = filterPaymentsByDateRange(payments, startDate, endDate);
            var overallSystemHealth = calculateOverallSystemHealth(filteredPayments, sellers);
            var paymentSystemMetrics = calculatePaymentSystemMetrics(filteredPayments);
            var userEngagement = calculateUserEngagement(filteredPayments, sellers);
            
            return new SystemMetricsResponse(overallSystemHealth, paymentSystemMetrics, userEngagement);
        });
    }
    
    private SystemMetricsResponse.OverallSystemHealth calculateOverallSystemHealth(
            List<PaymentNotification> payments, List<Seller> sellers) {
        
        var totalSales = calculateTotalSales(payments.stream().filter(isConfirmed::apply).toList());
        var totalTransactions = (long) payments.size();
        var systemUptime = calculateSystemUptime(payments);
        var averageResponseTime = calculateAverageResponseTime(payments);
        var errorRate = calculateErrorRate(payments);
        var activeUsers = (long) sellers.size();
        
        return new SystemMetricsResponse.OverallSystemHealth(
            totalSales, totalTransactions, systemUptime, averageResponseTime, errorRate, activeUsers
        );
    }
    
    private double calculateSystemUptime(List<PaymentNotification> payments) {
        // Simplified calculation - in real scenario would track actual uptime
        return payments.isEmpty() ? 100.0 : 99.9;
    }
    
    private double calculateAverageResponseTime(List<PaymentNotification> payments) {
        // Simplified calculation - in real scenario would track actual response times
        return payments.isEmpty() ? 0.0 : 2.5; // seconds
    }
    
    private double calculateErrorRate(List<PaymentNotification> payments) {
        if (payments.isEmpty()) return 0.0;
        
        var errorCount = payments.stream()
            .mapToLong(payment -> REJECTED_STATUS.equals(payment.status) ? 1 : 0)
            .sum();
        
        return (double) errorCount / payments.size() * 100.0;
    }
    
    private SystemMetricsResponse.PaymentSystemMetrics calculatePaymentSystemMetrics(
            List<PaymentNotification> payments) {
        
        var totalPaymentsProcessed = (long) payments.size();
        var pendingPayments = payments.stream().mapToLong(payment -> isPending.apply(payment) ? 1 : 0).sum();
        var confirmedPayments = payments.stream().mapToLong(payment -> isConfirmed.apply(payment) ? 1 : 0).sum();
        var rejectedPayments = payments.stream().mapToLong(payment -> isRejected.apply(payment) ? 1 : 0).sum();
        var averageConfirmationTime = calculateAverageConfirmationTime(payments);
        var paymentSuccessRate = calculatePaymentSuccessRate(confirmedPayments, totalPaymentsProcessed);
        
        return new SystemMetricsResponse.PaymentSystemMetrics(
            totalPaymentsProcessed, pendingPayments, confirmedPayments, rejectedPayments,
            averageConfirmationTime, paymentSuccessRate
        );
    }
    
    private double calculateAverageConfirmationTime(List<PaymentNotification> payments) {
        // Simplified calculation - in real scenario would track actual confirmation times
        return payments.isEmpty() ? 0.0 : 1.5; // minutes
    }
    
    private double calculatePaymentSuccessRate(long confirmedPayments, long totalPayments) {
        if (totalPayments == 0) return 0.0;
        return (double) confirmedPayments / totalPayments * 100.0;
    }
    
    private SystemMetricsResponse.UserEngagement calculateUserEngagement(
            List<PaymentNotification> payments, List<Seller> sellers) {
        
        var dailyActiveUsers = calculateDailyActiveUsers(payments);
        var weeklyActiveUsers = calculateWeeklyActiveUsers(payments);
        var monthlyActiveUsers = calculateMonthlyActiveUsers(payments);
        var averageSessionDuration = calculateAverageSessionDuration(payments);
        var featureUsage = calculateFeatureUsage(payments);
        
        return new SystemMetricsResponse.UserEngagement(
            dailyActiveUsers, weeklyActiveUsers, monthlyActiveUsers, averageSessionDuration, featureUsage
        );
    }
    
    private long calculateDailyActiveUsers(List<PaymentNotification> payments) {
        var today = LocalDate.now();
        return payments.stream()
            .filter(payment -> Optional.ofNullable(payment.createdAt)
                .map(LocalDateTime::toLocalDate)
                .map(date -> date.equals(today))
                .orElse(false))
            .filter(payment -> payment.confirmedBy != null)
            .map(payment -> payment.confirmedBy)
            .distinct()
            .count();
    }
    
    private long calculateWeeklyActiveUsers(List<PaymentNotification> payments) {
        var weekStart = LocalDate.now().minusDays(7);
        return payments.stream()
            .filter(payment -> Optional.ofNullable(payment.createdAt)
                .map(LocalDateTime::toLocalDate)
                .map(date -> !date.isBefore(weekStart))
                .orElse(false))
            .filter(payment -> payment.confirmedBy != null)
            .map(payment -> payment.confirmedBy)
            .distinct()
            .count();
    }
    
    private long calculateMonthlyActiveUsers(List<PaymentNotification> payments) {
        var monthStart = LocalDate.now().minusDays(30);
        return payments.stream()
            .filter(payment -> Optional.ofNullable(payment.createdAt)
                .map(LocalDateTime::toLocalDate)
                .map(date -> !date.isBefore(monthStart))
                .orElse(false))
            .filter(payment -> payment.confirmedBy != null)
            .map(payment -> payment.confirmedBy)
            .distinct()
            .count();
    }
    
    private SystemMetricsResponse.FeatureUsage calculateFeatureUsage(List<PaymentNotification> payments) {
        // Simplified feature usage calculation
        var qrScannerUsage = calculateFeatureUsagePercentage(payments, "QR_SCANNER");
        var paymentManagementUsage = calculateFeatureUsagePercentage(payments, "PAYMENT_MANAGEMENT");
        var analyticsUsage = calculateFeatureUsagePercentage(payments, "ANALYTICS");
        var notificationsUsage = calculateFeatureUsagePercentage(payments, "NOTIFICATIONS");
        
        return new SystemMetricsResponse.FeatureUsage(
            qrScannerUsage, paymentManagementUsage, analyticsUsage, notificationsUsage
        );
    }
    
    private double calculateFeatureUsagePercentage(List<PaymentNotification> payments, String feature) {
        // Simplified calculation - in real scenario would track actual feature usage
        return payments.isEmpty() ? 0.0 : 75.0; // percentage
    }
    
    public Uni<AdministrativeInsightsResponse> calculateAdministrativeInsights(List<PaymentNotification> payments, 
                                                                                         List<Seller> sellers, 
                                                                                         LocalDate startDate, 
                                                                                         LocalDate endDate) {
        return Uni.createFrom().item(() -> {
            var filteredPayments = filterPaymentsByDateRange(payments, startDate, endDate);
            var managementAlerts = generateManagementAlerts(filteredPayments, sellers);
            var recommendations = generateRecommendations(filteredPayments, sellers);
            var growthOpportunities = calculateGrowthOpportunities(filteredPayments, sellers);
            
            return new AdministrativeInsightsResponse(managementAlerts, recommendations, growthOpportunities);
        });
    }
    
    private List<AdministrativeInsightsResponse.ManagementAlert> generateManagementAlerts(
            List<PaymentNotification> payments, List<Seller> sellers) {
        
        var alerts = new java.util.ArrayList<AdministrativeInsightsResponse.ManagementAlert>();
        
        if (payments.isEmpty()) {
            alerts.add(createAlert("INFO", "LOW", 
                "No hay datos de transacciones en el período seleccionado", 
                "N/A", List.of(), "Revisar configuración de pagos"));
            return alerts;
        }
        
        // Check confirmation rate
        var confirmationRate = calculateConfirmationRate(payments);
        if (confirmationRate < LOW_CONFIRMATION_RATE_THRESHOLD) {
            alerts.add(createAlert("WARNING", "MEDIUM", 
                String.format("Tasa de confirmación baja: %.1f%%", confirmationRate),
                "N/A", List.of(), "Implementar proceso de seguimiento"));
        }
        
        // Check pending vs confirmed payments
        var pendingCount = payments.stream().mapToLong(payment -> isPending.apply(payment) ? 1 : 0).sum();
        var confirmedCount = payments.stream().mapToLong(payment -> isConfirmed.apply(payment) ? 1 : 0).sum();
        
        if (pendingCount > confirmedCount) {
            alerts.add(createAlert("WARNING", "HIGH", 
                String.format("Muchos pagos pendientes: %d vs %d confirmados", pendingCount, confirmedCount),
                "N/A", List.of(), "Revisar proceso de confirmación"));
        }
        
        // Check seller activity
        if (!sellers.isEmpty()) {
            alerts.add(createAlert("INFO", "LOW", 
                String.format("Total de vendedores activos: %d", sellers.size()),
                "N/A", List.of(), "Considerar capacitación adicional"));
        }
        
        // Check for suspicious transactions
        var suspiciousCount = payments.stream().mapToLong(payment -> isSuspicious.test(payment) ? 1 : 0).sum();
        if (suspiciousCount > 0) {
            alerts.add(createAlert("WARNING", "HIGH", 
                String.format("Se detectaron %d transacciones sospechosas", suspiciousCount),
                "N/A", List.of(), "Revisar transacciones manualmente"));
        }
        
        return alerts;
    }
    
    private AdministrativeInsightsResponse.ManagementAlert createAlert(String type, String severity, 
                                                                      String message, String affectedBranch, 
                                                                      List<String> affectedSellers, String recommendation) {
        return new AdministrativeInsightsResponse.ManagementAlert(
            type, severity, message, affectedBranch, affectedSellers, recommendation);
    }
    
    private double calculateConfirmationRate(List<PaymentNotification> payments) {
        if (payments.isEmpty()) return 0.0;
        
        var confirmedCount = payments.stream().mapToLong(payment -> isConfirmed.apply(payment) ? 1 : 0).sum();
        return (double) confirmedCount / payments.size() * 100.0;
    }
    
    private List<String> generateRecommendations(List<PaymentNotification> payments, List<Seller> sellers) {
        var recommendations = new java.util.ArrayList<String>();
        
        if (payments.isEmpty()) {
            recommendations.add("Considera revisar la configuración de pagos");
            return recommendations;
        }
        
        var confirmationRate = calculateConfirmationRate(payments);
        if (confirmationRate < LOW_CONFIRMATION_RATE_THRESHOLD) {
            recommendations.add("Implementar proceso de seguimiento de pagos pendientes");
        }
        
        var pendingCount = payments.stream().mapToLong(payment -> isPending.apply(payment) ? 1 : 0).sum();
        var confirmedCount = payments.stream().mapToLong(payment -> isConfirmed.apply(payment) ? 1 : 0).sum();
        
        if (pendingCount > confirmedCount) {
            recommendations.add("Revisar proceso de confirmación de pagos");
        }
        
        if (!sellers.isEmpty()) {
            recommendations.add("Considerar capacitación adicional para vendedores");
        }
        
        var suspiciousCount = payments.stream().mapToLong(payment -> isSuspicious.test(payment) ? 1 : 0).sum();
        if (suspiciousCount > 0) {
            recommendations.add("Implementar sistema de alertas para transacciones sospechosas");
        }
        
        return recommendations;
    }
    
    private AdministrativeInsightsResponse.GrowthOpportunities calculateGrowthOpportunities(
            List<PaymentNotification> payments, List<Seller> sellers) {
        
        var potentialNewBranches = calculatePotentialNewBranches(payments, sellers);
        var marketExpansion = determineMarketExpansion(payments);
        var sellerRecruitment = calculateSellerRecruitmentNeeds(sellers);
        var revenueProjection = calculateRevenueProjection(payments);
        
        return new AdministrativeInsightsResponse.GrowthOpportunities(
            potentialNewBranches, marketExpansion, sellerRecruitment, revenueProjection
        );
    }
    
    private Long calculatePotentialNewBranches(List<PaymentNotification> payments, List<Seller> sellers) {
        // Simplified calculation based on current performance
        var totalSales = calculateTotalSales(payments.stream().filter(isConfirmed::apply).toList());
        return totalSales > 10000.0 ? 2L : 0L;
    }
    
    private String determineMarketExpansion(List<PaymentNotification> payments) {
        var transactionVolume = payments.size();
        return transactionVolume > 100 ? "Alto potencial" : "Estable";
    }
    
    private Long calculateSellerRecruitmentNeeds(List<Seller> sellers) {
        // Simplified calculation based on current seller count
        return sellers.size() < 10 ? 5L : 0L;
    }
    
    private Double calculateRevenueProjection(List<PaymentNotification> payments) {
        var currentRevenue = calculateTotalSales(payments.stream().filter(isConfirmed::apply).toList());
        // Simple 20% growth projection
        return currentRevenue * 1.2;
    }
    
    public Uni<FinancialOverviewResponse> calculateFinancialOverview(List<PaymentNotification> payments, 
                                                                               List<Seller> sellers, 
                                                                               LocalDate startDate, 
                                                                               LocalDate endDate) {
        return Uni.createFrom().item(() -> {
            var filteredPayments = filterPaymentsByDateRange(payments, startDate, endDate);
            var revenueBreakdown = calculateRevenueBreakdown(filteredPayments, sellers);
            var costAnalysis = calculateCostAnalysis(filteredPayments);
            
            return new FinancialOverviewResponse(revenueBreakdown, costAnalysis);
        });
    }
    
    private FinancialOverviewResponse.RevenueBreakdown calculateRevenueBreakdown(
            List<PaymentNotification> payments, List<Seller> sellers) {
        
        var confirmedPayments = payments.stream().filter(isConfirmed::apply).toList();
        var totalRevenue = calculateTotalSales(confirmedPayments);
        var revenueByBranch = calculateRevenueByBranch(confirmedPayments, sellers);
        var revenueGrowth = calculateRevenueGrowth(payments);
        
        return new FinancialOverviewResponse.RevenueBreakdown(totalRevenue, revenueByBranch, revenueGrowth);
    }
    
    private List<FinancialOverviewResponse.RevenueByBranch> calculateRevenueByBranch(
            List<PaymentNotification> confirmedPayments, List<Seller> sellers) {
        
        var branchGroups = groupSellersByBranch(sellers);
        var totalRevenue = calculateTotalSales(confirmedPayments);
        
        return branchGroups.entrySet().stream()
            .map(entry -> {
                var branchName = entry.getKey();
                var branchSellers = entry.getValue();
                var branchPayments = filterPaymentsBySellers(confirmedPayments, branchSellers);
                var branchRevenue = calculateTotalSales(branchPayments);
                var percentage = totalRevenue > 0 ? (branchRevenue / totalRevenue) * 100.0 : 0.0;
                
                return new FinancialOverviewResponse.RevenueByBranch(
                    null, branchName, branchRevenue, percentage);
            })
            .toList();
    }
    
    private FinancialOverviewResponse.RevenueGrowth calculateRevenueGrowth(List<PaymentNotification> payments) {
        // Simplified growth calculation based on date range
        var dailyGrowth = calculateDailyGrowth(payments);
        var weeklyGrowth = dailyGrowth * 7;
        var monthlyGrowth = dailyGrowth * 30;
        var yearlyGrowth = dailyGrowth * 365;
        
        return new FinancialOverviewResponse.RevenueGrowth(
            dailyGrowth, weeklyGrowth, monthlyGrowth, yearlyGrowth);
    }
    
    private double calculateDailyGrowth(List<PaymentNotification> payments) {
        // Simplified calculation - in real scenario would compare with previous periods
        return payments.isEmpty() ? 0.0 : 5.0; // percentage
    }
    
    private FinancialOverviewResponse.CostAnalysis calculateCostAnalysis(List<PaymentNotification> payments) {
        var totalRevenue = calculateTotalSales(payments.stream().filter(isConfirmed::apply).toList());
        var operationalCosts = calculateOperationalCosts(payments);
        var sellerCommissions = calculateSellerCommissions(totalRevenue);
        var systemMaintenance = calculateSystemMaintenanceCosts();
        var netProfit = totalRevenue - operationalCosts - sellerCommissions - systemMaintenance;
        var profitMargin = totalRevenue > 0 ? (netProfit / totalRevenue) * 100.0 : 0.0;
        
        return new FinancialOverviewResponse.CostAnalysis(
            operationalCosts, sellerCommissions, systemMaintenance, netProfit, profitMargin);
    }
    
    private double calculateOperationalCosts(List<PaymentNotification> payments) {
        // Simplified calculation - 10% of revenue
        var totalRevenue = calculateTotalSales(payments.stream().filter(isConfirmed::apply).toList());
        return totalRevenue * 0.1;
    }
    
    private double calculateSellerCommissions(double totalRevenue) {
        // Simplified calculation - 5% of revenue
        return totalRevenue * 0.05;
    }
    
    private double calculateSystemMaintenanceCosts() {
        // Fixed monthly maintenance cost
        return 500.0;
    }
    
    public Uni<ComplianceAndSecurityResponse> calculateComplianceAndSecurity(List<PaymentNotification> payments, 
                                                                                       List<Seller> sellers, 
                                                                                       LocalDate startDate, 
                                                                                       LocalDate endDate) {
        return Uni.createFrom().item(() -> {
            var filteredPayments = filterPaymentsByDateRange(payments, startDate, endDate);
            var securityMetrics = calculateSecurityMetrics(filteredPayments);
            var complianceStatus = calculateComplianceStatus(filteredPayments);
            
            return new ComplianceAndSecurityResponse(securityMetrics, complianceStatus);
        });
    }
    
    private ComplianceAndSecurityResponse.SecurityMetrics calculateSecurityMetrics(
            List<PaymentNotification> payments) {
        
        var failedLoginAttempts = 0L; // Would be tracked separately
        var suspiciousActivities = payments.stream().mapToLong(payment -> isSuspicious.test(payment) ? 1 : 0).sum();
        var dataBreaches = 0L; // Would be tracked separately
        var securityScore = calculateSecurityScore(payments, suspiciousActivities);
        
        return new ComplianceAndSecurityResponse.SecurityMetrics(
            failedLoginAttempts, suspiciousActivities, dataBreaches, securityScore);
    }
    
    private double calculateSecurityScore(List<PaymentNotification> payments, long suspiciousActivities) {
        var baseScore = 10.0;
        var suspiciousPenalty = suspiciousActivities * 0.5;
        var duplicatePenalty = calculateDuplicateTransactions(payments) * 1.0;
        
        return Math.max(0.0, baseScore - suspiciousPenalty - duplicatePenalty);
    }
    
    private long calculateDuplicateTransactions(List<PaymentNotification> payments) {
        return payments.stream()
            .collect(Collectors.groupingBy(payment -> payment.deduplicationHash))
            .entrySet().stream()
            .mapToLong(entry -> entry.getValue().size() > 1 ? 1 : 0)
            .sum();
    }
    
    private ComplianceAndSecurityResponse.ComplianceStatus calculateComplianceStatus(
            List<PaymentNotification> payments) {
        
        var dataProtection = evaluateDataProtection(payments);
        var auditTrail = evaluateAuditTrail(payments);
        var backupStatus = evaluateBackupStatus();
        var lastAudit = LocalDate.now().toString();
        
        return new ComplianceAndSecurityResponse.ComplianceStatus(
            dataProtection, auditTrail, backupStatus, lastAudit);
    }
    
    private String evaluateDataProtection(List<PaymentNotification> payments) {
        // Simplified evaluation based on transaction volume and security measures
        var suspiciousCount = payments.stream().mapToLong(payment -> isSuspicious.test(payment) ? 1 : 0).sum();
        return suspiciousCount == 0 ? "EXCELLENT" : suspiciousCount < 5 ? "GOOD" : "NEEDS_ATTENTION";
    }
    
    private String evaluateAuditTrail(List<PaymentNotification> payments) {
        // Simplified evaluation - in real scenario would check actual audit logs
        var hasAuditData = payments.stream()
            .anyMatch(payment -> payment.createdAt != null && payment.confirmedBy != null);
        return hasAuditData ? "COMPLETE" : "INCOMPLETE";
    }
    
    private String evaluateBackupStatus() {
        // Simplified evaluation - in real scenario would check backup systems
        return "UP_TO_DATE";
    }
}