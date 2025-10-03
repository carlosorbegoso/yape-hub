package org.sky.service.stats.calculators.seller.achievements;

import jakarta.enterprise.context.ApplicationScoped;

import org.sky.dto.request.stats.SellerAnalyticsRequest;
import org.sky.dto.response.seller.SellerAchievements;
import org.sky.dto.response.seller.Milestone;
import org.sky.dto.response.seller.Badge;
import org.sky.model.PaymentNotificationEntity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class SellerAchievementsCalculator {
    
    private static final String CONFIRMED_STATUS = "CONFIRMED";
    
    public SellerAchievements calculateSellerAchievements(List<PaymentNotificationEntity> sellerPayments,
                                                                                   List<PaymentNotificationEntity> allPayments,
                                                                                   SellerAnalyticsRequest request) {
        if (sellerPayments.isEmpty()) {
            return new SellerAchievements(0L, 0L, 0L, List.of(), List.of());
        }
        
        // Calcular logros usando days y period
        var streakDays = calculateStreakDays(sellerPayments, request.endDate(), request.days());
        var bestStreak = calculateBestStreak(sellerPayments, request.startDate(), request.endDate());
        var totalStreaks = calculateTotalStreaks(sellerPayments, request.startDate(), request.endDate());
        
        // Crear hitos usando include para determinar qué incluir
        var milestones = createMilestones(sellerPayments, request.include(), request.metric());
        
        // Crear badges usando include
        var badges = createBadges(sellerPayments, request.include());
        
        return new SellerAchievements(streakDays, bestStreak, totalStreaks, milestones, badges);
    }
    
    private long calculateStreakDays(List<PaymentNotificationEntity> payments, LocalDate endDate, Integer days) {
        if (payments.isEmpty()) return 0L;
        
        // Calcular racha actual basada en días consecutivos con ventas
        var confirmedPayments = filterPaymentsByStatus(payments, CONFIRMED_STATUS);
        var maxDays = days != null ? days : 30;
        
        // Implementación simplificada - contar días con ventas en el período
        var daysWithSales = confirmedPayments.stream()
                .map(p -> p.createdAt.toLocalDate())
                .distinct()
                .count();
        
        return Math.min(daysWithSales, maxDays);
    }
    
    private long calculateBestStreak(List<PaymentNotificationEntity> payments, LocalDate startDate, LocalDate endDate) {
        if (payments.isEmpty()) return 0L;
        
        var confirmedPayments = filterPaymentsByStatus(payments, CONFIRMED_STATUS);
        var daysWithSales = confirmedPayments.stream()
                .filter(p -> !p.createdAt.toLocalDate().isBefore(startDate))
                .filter(p -> !p.createdAt.toLocalDate().isAfter(endDate))
                .map(p -> p.createdAt.toLocalDate())
                .distinct()
                .count();
        
        return Math.max(daysWithSales, 1L);
    }
    
    private long calculateTotalStreaks(List<PaymentNotificationEntity> payments, LocalDate startDate, LocalDate endDate) {
        if (payments.isEmpty()) return 0L;
        
        // Contar períodos de actividad
        var confirmedPayments = filterPaymentsByStatus(payments, CONFIRMED_STATUS);
        var activeDays = confirmedPayments.stream()
                .filter(p -> !p.createdAt.toLocalDate().isBefore(startDate))
                .filter(p -> !p.createdAt.toLocalDate().isAfter(endDate))
                .map(p -> p.createdAt.toLocalDate())
                .distinct()
                .count();
        
        // Una racha por cada 7 días de actividad
        return Math.max(activeDays / 7, 1L);
    }
    
    private List<Milestone> createMilestones(List<PaymentNotificationEntity> payments, String include, String metric) {
        var milestones = new ArrayList<Milestone>();
        
        if (shouldIncludeMetric("milestones", include)) {
            var confirmedPayments = filterPaymentsByStatus(payments, CONFIRMED_STATUS);
            
            // Primer hito: Primera venta
            var firstPayment = confirmedPayments.stream()
                    .min(java.util.Comparator.comparing(p -> p.createdAt))
                    .orElse(null);
            
            if (firstPayment != null) {
                milestones.add(new Milestone(
                        "first_sale",
                        firstPayment.createdAt.toLocalDate().toString(),
                        true,
                        1.0
                ));
            }
            
            // Segundo hito: 10 ventas
            if (confirmedPayments.size() >= 10) {
                var tenthPayment = confirmedPayments.stream()
                        .sorted(java.util.Comparator.comparing(p -> p.createdAt))
                        .skip(9)
                        .findFirst()
                        .orElse(null);
                
                if (tenthPayment != null) {
                    milestones.add(new Milestone(
                            "ten_sales",
                            tenthPayment.createdAt.toLocalDate().toString(),
                            true,
                            10.0
                    ));
                }
            }
            
            // Tercer hito: 100 ventas
            if (confirmedPayments.size() >= 100) {
                var hundredthPayment = confirmedPayments.stream()
                        .sorted(java.util.Comparator.comparing(p -> p.createdAt))
                        .skip(99)
                        .findFirst()
                        .orElse(null);
                
                if (hundredthPayment != null) {
                    milestones.add(new Milestone(
                            "hundred_sales",
                            hundredthPayment.createdAt.toLocalDate().toString(),
                            true,
                            100.0
                    ));
                }
            }
        }
        
        return milestones;
    }
    
    private List<Badge> createBadges(List<PaymentNotificationEntity> payments, String include) {
        var badges = new ArrayList<Badge>();
        
        if (shouldIncludeMetric("badges", include)) {
            var confirmedPayments = filterPaymentsByStatus(payments, CONFIRMED_STATUS);
            
            // Badge: Primera Venta
            if (confirmedPayments.size() >= 1) {
                var firstPayment = confirmedPayments.stream()
                        .min(java.util.Comparator.comparing(p -> p.createdAt))
                        .orElse(null);
                
                if (firstPayment != null) {
                    badges.add(new Badge(
                            "Primera Venta",
                            "🎉",
                            "Completaste tu primera venta",
                            true,
                            firstPayment.createdAt.toLocalDate().toString()
                    ));
                }
            }
            
            // Badge: Vendedor Activo
            if (confirmedPayments.size() >= 10) {
                badges.add(new Badge(
                        "Vendedor Activo",
                        "⭐",
                        "Has completado 10 ventas",
                        true,
                        LocalDate.now().toString()
                ));
            }
            
            // Badge: Experto en Ventas
            if (confirmedPayments.size() >= 50) {
                badges.add(new Badge(
                        "Experto en Ventas",
                        "🏆",
                        "Has completado 50 ventas",
                        true,
                        LocalDate.now().toString()
                ));
            }
            
            // Badge: Maestro Vendedor
            if (confirmedPayments.size() >= 100) {
                badges.add(new Badge(
                        "Maestro Vendedor",
                        "👑",
                        "Has completado 100 ventas",
                        true,
                        LocalDate.now().toString()
                ));
            }
        }
        
        return badges;
    }
    
    private List<PaymentNotificationEntity> filterPaymentsByStatus(List<PaymentNotificationEntity> payments, String status) {
        return payments.stream()
                .filter(payment -> status.equals(payment.status))
                .toList();
    }
    
    private boolean shouldIncludeMetric(String metricType, String include) {
        if (include == null) return true;
        return include.contains(metricType) || include.equals("all");
    }
}
