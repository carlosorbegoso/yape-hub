package org.sky.service.stats.calculators.strategy;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.sky.model.PaymentNotificationEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Strategy Pattern: Calcula resumen financiero
 * Clean Code: Responsabilidad única - solo calcula métricas financieras
 */
@ApplicationScoped
public class FinancialOverviewStrategy implements CalculationStrategy<Map<String, Object>> {

    private static final Logger log = Logger.getLogger(FinancialOverviewStrategy.class);
    private static final int PARALLEL_THRESHOLD = 1000;

    @Override
    public Uni<Map<String, Object>> calculate(List<PaymentNotificationEntity> payments,
                                            LocalDate startDate,
                                            LocalDate endDate,
                                            Long adminId) {
        return Uni.createFrom().item(() -> {
            log.debug("🔄 FinancialOverviewStrategy: Calculando resumen financiero para " + payments.size() + " pagos");

            // Si no hay pagos, retornar overview por defecto
            if (payments == null || payments.isEmpty()) {
                log.debug("⚠️ FinancialOverviewStrategy: No hay pagos, usando overview por defecto");
                return Map.<String, Object>of(
                    "revenueBreakdown", Map.of(
                        "totalRevenue", 0.0,
                        "revenueByBranch", List.of(),
                        "revenueGrowth", Map.of(
                            "daily", 0.0,
                            "weekly", 0.0,
                            "monthly", 0.0,
                            "yearly", 0.0
                        )
                    ),
                    "costAnalysis", Map.of(
                        "operationalCosts", 0.0,
                        "sellerCommissions", 0.0,
                        "systemMaintenance", 5000.0,
                        "netProfit", -5000.0,
                        "profitMargin", 0.0
                    )
                );
            }

            var stream = payments.size() > PARALLEL_THRESHOLD ? 
                payments.parallelStream() : payments.stream();

            // Calcular ingresos totales
            double totalRevenue = stream
                .filter(p -> p.amount != null && p.amount > 0)
                .mapToDouble(p -> p.amount)
                .sum();

            long totalTransactions = payments.size();

            // Calcular costos operacionales (simulados)
            double operationalCosts = totalRevenue * 0.15; // 15% de los ingresos
            double sellerCommissions = totalRevenue * 0.08; // 8% de comisiones
            double systemMaintenance = 5000.0; // Costo fijo de mantenimiento
            
            // Calcular ganancia neta y margen
            double netProfit = totalRevenue - operationalCosts - sellerCommissions - systemMaintenance;
            double profitMargin = totalRevenue > 0 ? (netProfit / totalRevenue) * 100 : 0.0;

            // Crecimiento de ingresos (simulado)
            Map<String, Object> revenueGrowth = Map.of(
                "daily", 0.0,
                "weekly", 0.0,
                "monthly", 0.0,
                "yearly", 0.0
            );

            // Desglose de ingresos por branch
            List<Map<String, Object>> revenueByBranch;
            
            if (payments.isEmpty()) {
                // Si no hay pagos, generar datos por defecto con adminId
                revenueByBranch = List.of(Map.<String, Object>of(
                    "branchId", adminId,
                    "branchName", "Branch " + adminId, 
                    "revenue", totalRevenue,
                    "percentage", 100.0
                ));
            } else {
                // Agrupar por branch usando adminId de los pagos
                revenueByBranch = payments.stream()
                    .collect(Collectors.groupingBy(
                        payment -> payment.adminId,
                        Collectors.summingDouble(payment -> payment.amount.doubleValue())
                    ))
                    .entrySet().stream()
                    .map(entry -> {
                        Long branchId = entry.getKey();
                        String branchName = "Branch " + branchId;
                        Double branchRevenue = entry.getValue();
                        Double percentage = totalRevenue > 0 ? (branchRevenue / totalRevenue) * 100 : 0.0;
                        
                        return Map.<String, Object>of(
                            "branchId", branchId,
                            "branchName", branchName,
                            "revenue", branchRevenue,
                            "percentage", percentage
                        );
                    })
                    .collect(Collectors.toList());
                
                // Si no revenueByBranch está vacío, generar datos por defecto
                if (revenueByBranch.isEmpty()) {
                    revenueByBranch = List.of(Map.<String, Object>of(
                        "branchId", adminId != null ? adminId : 605L,
                        "branchName", "Branch " + (adminId != null ? adminId : 605), 
                        "revenue", totalRevenue,
                        "percentage", 100.0
                    ));
                }
            }
            
            // Desglose de ingresos
            Map<String, Object> revenueBreakdown = Map.of(
                "totalRevenue", totalRevenue,
                "revenueByBranch", revenueByBranch,
                "revenueGrowth", revenueGrowth
            );

            // Análisis de costos
            Map<String, Object> costAnalysis = Map.of(
                "operationalCosts", operationalCosts,
                "sellerCommissions", sellerCommissions,
                "systemMaintenance", systemMaintenance,
                "netProfit", netProfit,
                "profitMargin", profitMargin
            );

            log.debug("✅ FinancialOverviewStrategy: Resumen financiero calculado - Ingresos: " + totalRevenue);

            return Map.<String, Object>of(
                "revenueBreakdown", revenueBreakdown,
                "costAnalysis", costAnalysis
            );
        });
    }

    @Override
    public boolean canHandle(List<PaymentNotificationEntity> payments, 
                           LocalDate startDate, 
                           LocalDate endDate, 
                           Long adminId) {
        return payments != null; // Permitir listas vacías para calcular overview por defecto
    }

    @Override
    public String getStrategyName() {
        return "FinancialOverviewStrategy";
    }
    
}
