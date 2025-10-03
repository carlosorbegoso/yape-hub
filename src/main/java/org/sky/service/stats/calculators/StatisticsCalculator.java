package org.sky.service.stats.calculators;

import io.smallrye.mutiny.Uni;
import org.sky.model.PaymentNotificationEntity;
import org.sky.dto.response.stats.*;
// Imports removidos para evitar warnings
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

@ApplicationScoped
public class StatisticsCalculator {

    private static final Logger log = Logger.getLogger(StatisticsCalculator.class);
    
    // Configuración para streams paralelos
    private static final int PARALLEL_THRESHOLD = 1000; // Umbral para usar procesamiento paralelo
    
    // Calculadores especializados eliminados - no se utilizaban
    

    /**
     * Calcula estadísticas básicas de manera paralela usando Mutiny
     */
    public Uni<BasicStats> calculateBasicStats(List<PaymentNotificationEntity> payments) {
        return Uni.createFrom().item(() -> {
            log.debug("🔄 Calculando estadísticas básicas en paralelo para " + payments.size() + " pagos");
            
            // Usar streams paralelos para cálculos intensivos
            var stream = payments.size() > PARALLEL_THRESHOLD ? payments.parallelStream() : payments.stream();
            
            double totalSales = stream
                .filter(p -> p.amount != null && p.amount > 0)
                .mapToDouble(p -> p.amount)
                .sum();
            
            long totalTransactions = payments.size();
            double averageTransactionValue = totalTransactions > 0 ? totalSales / totalTransactions : 0.0;
            
            log.debug("✅ Estadísticas básicas calculadas: Ventas=" + totalSales + ", Transacciones=" + totalTransactions);
            return new BasicStats(totalSales, totalTransactions, averageTransactionValue);
        });
    }

    /**
     * Calcula métricas de rendimiento de manera paralela usando Mutiny
     */
    public Uni<PerformanceMetrics> calculatePerformanceMetrics(List<PaymentNotificationEntity> payments) {
        // Usar Uni.combine para ejecutar cálculos en paralelo
        return Uni.combine()
            .all()
            .unis(
                // Calcular conteos en paralelo
                Uni.createFrom().item(() -> {
                    var stream = payments.size() > PARALLEL_THRESHOLD ? payments.parallelStream() : payments.stream();
                    return stream.filter(p -> "CONFIRMED".equals(p.status)).count();
                }),
                Uni.createFrom().item(() -> {
                    var stream = payments.size() > PARALLEL_THRESHOLD ? payments.parallelStream() : payments.stream();
                    return stream.filter(p -> "REJECTED".equals(p.status)).count();
                }),
                Uni.createFrom().item(() -> {
                    var stream = payments.size() > PARALLEL_THRESHOLD ? payments.parallelStream() : payments.stream();
                    return stream.filter(p -> "PENDING".equals(p.status)).count();
                }),
                // Calcular tiempo promedio de confirmación en paralelo
                Uni.createFrom().item(() -> {
                    var stream = payments.size() > PARALLEL_THRESHOLD ? payments.parallelStream() : payments.stream();
                    return stream.filter(p -> "CONFIRMED".equals(p.status) && p.updatedAt != null)
                        .mapToLong(p -> java.time.Duration.between(p.createdAt, p.updatedAt).toMinutes())
                        .average()
                        .orElse(0.0);
                })
            )
            .with((confirmedCount, rejectedCount, pendingCount, avgConfirmationTime) -> {
                log.debug("🔄 Calculando métricas de rendimiento en paralelo para " + payments.size() + " pagos");
                
                double claimRate = payments.size() > 0 ? (double) confirmedCount / payments.size() : 0.0;
                double rejectionRate = payments.size() > 0 ? (double) rejectedCount / payments.size() : 0.0;
                
                log.debug("✅ Métricas de rendimiento calculadas: Confirmados=" + confirmedCount + 
                         ", Rechazados=" + rejectedCount + ", Pendientes=" + pendingCount);
                
                return new PerformanceMetrics(
                    avgConfirmationTime,
                    claimRate,
                    rejectionRate,
                    pendingCount.intValue(),
                    confirmedCount.intValue(),
                    rejectedCount.intValue()
                );
            });
    }

    /**
     * Calcula datos de ventas diarias de manera paralela usando Mutiny
     */
    public Uni<List<DailySalesData>> calculateDailySalesData(List<PaymentNotificationEntity> payments, 
                                                           LocalDate startDate, LocalDate endDate) {
        return Uni.createFrom().item(() -> {
            log.debug("🔄 Calculando ventas diarias en paralelo para " + payments.size() + " pagos");
            
            // Usar ConcurrentHashMap para thread-safety en procesamiento paralelo
            Map<LocalDate, Double> dailySales = new ConcurrentHashMap<>();
            Map<LocalDate, Long> dailyTransactions = new ConcurrentHashMap<>();

            // Procesar pagos en paralelo con streams
            var stream = payments.size() > PARALLEL_THRESHOLD ? payments.parallelStream() : payments.stream();
            stream.forEach(payment -> {
                LocalDate date = payment.createdAt.toLocalDate();
                if (date.isAfter(startDate.minusDays(1)) && date.isBefore(endDate.plusDays(1))) {
                    double amount = (payment.amount != null && payment.amount > 0) ? payment.amount : 0.0;
                    dailySales.merge(date, amount, Double::sum);
                    dailyTransactions.merge(date, 1L, Long::sum);
                }
            });

            // Generar datos para todos los días del rango en paralelo
            List<LocalDate> dateRange = startDate.datesUntil(endDate.plusDays(1))
                .collect(Collectors.toList());
            
            List<DailySalesData> result = dateRange.parallelStream()
                .map(currentDate -> {
                    double sales = dailySales.getOrDefault(currentDate, 0.0);
                    long transactions = dailyTransactions.getOrDefault(currentDate, 0L);
                    return new DailySalesData(
                        currentDate.toString(),
                        currentDate.getDayOfWeek().toString(),
                        sales,
                        transactions
                    );
                })
                .collect(Collectors.toList());

            log.debug("✅ Ventas diarias calculadas: " + result.size() + " días");
            return result;
        });
    }

    /**
     * Calcula datos de ventas por hora de manera paralela usando Mutiny
     */
    public Uni<List<HourlySalesData>> calculateHourlySalesData(List<PaymentNotificationEntity> payments) {
        return Uni.createFrom().item(() -> {
            log.debug("🔄 Calculando ventas por hora en paralelo para " + payments.size() + " pagos");
            
            // Usar ConcurrentHashMap para thread-safety
            Map<Integer, Double> hourlySales = new ConcurrentHashMap<>();
            Map<Integer, Long> hourlyTransactions = new ConcurrentHashMap<>();

            // Procesar pagos en paralelo
            var stream = payments.size() > PARALLEL_THRESHOLD ? payments.parallelStream() : payments.stream();
            stream.forEach(payment -> {
                int hour = payment.createdAt.getHour();
                double amount = (payment.amount != null && payment.amount > 0) ? payment.amount : 0.0;
                hourlySales.merge(hour, amount, Double::sum);
                hourlyTransactions.merge(hour, 1L, Long::sum);
            });

            // Generar datos para todas las horas en paralelo
            List<HourlySalesData> result = IntStream.range(0, 24)
                .parallel()
                .mapToObj(hour -> {
                    double sales = hourlySales.getOrDefault(hour, 0.0);
                    long transactions = hourlyTransactions.getOrDefault(hour, 0L);
                    return new HourlySalesData(String.valueOf(hour), sales, transactions);
                })
                .collect(Collectors.toList());

            log.debug("✅ Ventas por hora calculadas: " + result.size() + " horas");
            return result;
        });
    }

    /**
     * Calcula datos de ventas semanales de manera paralela usando Mutiny
     */
    public Uni<List<WeeklySalesData>> calculateWeeklySalesData(List<PaymentNotificationEntity> payments, 
                                                             LocalDate startDate, LocalDate endDate) {
        return Uni.createFrom().item(() -> {
            log.debug("🔄 Calculando ventas semanales en paralelo para " + payments.size() + " pagos");
            
            // Usar ConcurrentHashMap para thread-safety
            Map<LocalDate, Double> weeklySales = new ConcurrentHashMap<>();
            Map<LocalDate, Long> weeklyTransactions = new ConcurrentHashMap<>();

            // Procesar pagos en paralelo
            var stream = payments.size() > PARALLEL_THRESHOLD ? payments.parallelStream() : payments.stream();
            stream.forEach(payment -> {
                LocalDate date = payment.createdAt.toLocalDate();
                LocalDate weekStart = date.with(DayOfWeek.MONDAY);
                if (weekStart.isAfter(startDate.minusWeeks(1)) && weekStart.isBefore(endDate.plusWeeks(1))) {
                    double amount = (payment.amount != null && payment.amount > 0) ? payment.amount : 0.0;
                    weeklySales.merge(weekStart, amount, Double::sum);
                    weeklyTransactions.merge(weekStart, 1L, Long::sum);
                }
            });

            // Generar datos para todas las semanas en paralelo
            List<LocalDate> weekRange = new ArrayList<>();
            LocalDate currentWeek = startDate.with(DayOfWeek.MONDAY);
            while (!currentWeek.isAfter(endDate)) {
                weekRange.add(currentWeek);
                currentWeek = currentWeek.plusWeeks(1);
            }
            
            List<WeeklySalesData> result = weekRange.parallelStream()
                .map(weekStart -> {
                    double sales = weeklySales.getOrDefault(weekStart, 0.0);
                    long transactions = weeklyTransactions.getOrDefault(weekStart, 0L);
                    return new WeeklySalesData(weekStart.toString(), sales, transactions);
                })
                .collect(Collectors.toList());

            log.debug("✅ Ventas semanales calculadas: " + result.size() + " semanas");
            return result;
        });
    }

    /**
     * Calcula datos de ventas mensuales de manera paralela usando Mutiny
     */
    public Uni<List<MonthlySalesData>> calculateMonthlySalesData(List<PaymentNotificationEntity> payments, 
                                                               LocalDate startDate, LocalDate endDate) {
        return Uni.createFrom().item(() -> {
            log.debug("🔄 Calculando ventas mensuales en paralelo para " + payments.size() + " pagos");
            
            // Usar ConcurrentHashMap para thread-safety
            Map<String, Double> monthlySales = new ConcurrentHashMap<>();
            Map<String, Long> monthlyTransactions = new ConcurrentHashMap<>();

            // Procesar pagos en paralelo
            var stream = payments.size() > PARALLEL_THRESHOLD ? payments.parallelStream() : payments.stream();
            stream.forEach(payment -> {
                LocalDate date = payment.createdAt.toLocalDate();
                String monthKey = date.getYear() + "-" + String.format("%02d", date.getMonthValue());
                if (date.isAfter(startDate.minusMonths(1)) && date.isBefore(endDate.plusMonths(1))) {
                    double amount = (payment.amount != null && payment.amount > 0) ? payment.amount : 0.0;
                    monthlySales.merge(monthKey, amount, Double::sum);
                    monthlyTransactions.merge(monthKey, 1L, Long::sum);
                }
            });

            // Generar datos para todos los meses en paralelo
            List<String> monthRange = new ArrayList<>();
            LocalDate currentMonth = startDate.withDayOfMonth(1);
            while (!currentMonth.isAfter(endDate)) {
                String monthKey = currentMonth.getYear() + "-" + String.format("%02d", currentMonth.getMonthValue());
                monthRange.add(monthKey);
                currentMonth = currentMonth.plusMonths(1);
            }
            
            List<MonthlySalesData> result = monthRange.parallelStream()
                .map(monthKey -> {
                    double sales = monthlySales.getOrDefault(monthKey, 0.0);
                    long transactions = monthlyTransactions.getOrDefault(monthKey, 0L);
                    return new MonthlySalesData(monthKey, sales, transactions);
                })
                .collect(Collectors.toList());

            log.debug("✅ Ventas mensuales calculadas: " + result.size() + " meses");
            return result;
        });
    }

    /**
     * Calcula datos de top sellers de manera paralela usando Mutiny
     */
    public Uni<List<TopSellerData>> calculateTopSellersData(List<PaymentNotificationEntity> payments, Long adminId) {
        return Uni.createFrom().item(() -> {
            log.debug("🔄 Calculando top sellers en paralelo para " + payments.size() + " pagos");
            
            // Usar ConcurrentHashMap para thread-safety
            Map<Long, Double> sellerSales = new ConcurrentHashMap<>();
            Map<Long, Long> sellerTransactions = new ConcurrentHashMap<>();

            // Procesar pagos en paralelo
            var stream = payments.size() > PARALLEL_THRESHOLD ? payments.parallelStream() : payments.stream();
            stream.filter(payment -> payment.adminId != null && payment.amount != null && payment.amount > 0)
                .forEach(payment -> {
                    Long sellerId = payment.adminId;
                    sellerSales.merge(sellerId, payment.amount, Double::sum);
                    sellerTransactions.merge(sellerId, 1L, Long::sum);
                });

            // Generar lista de top sellers en paralelo
            List<TopSellerData> result = sellerSales.entrySet().parallelStream()
                .map(entry -> {
                    Long sellerId = entry.getKey();
                    Double sales = entry.getValue();
                    Long transactions = sellerTransactions.getOrDefault(sellerId, 0L);
                    return new TopSellerData(
                        0, // rank se asignará después
                        sellerId,
                        "Seller " + sellerId,
                        "Branch " + sellerId,
                        sales,
                        transactions
                    );
                })
                .sorted((a, b) -> Double.compare(b.totalSales(), a.totalSales()))
                .limit(10)
                .collect(Collectors.toList());

            // Asignar ranks
            for (int i = 0; i < result.size(); i++) {
                TopSellerData seller = result.get(i);
                result.set(i, new TopSellerData(
                    i + 1,
                    seller.sellerId(),
                    seller.sellerName(),
                    seller.branchName(),
                    seller.totalSales(),
                    seller.transactionCount()
                ));
            }

            log.debug("✅ Top sellers calculados: " + result.size() + " sellers");
            return result;
        });
    }

    /**
     * Calcula todas las estadísticas en paralelo usando Mutiny
     * Este método ejecuta todos los cálculos de forma paralela para máxima eficiencia
     */
    public Uni<ParallelStatsResult> calculateAllStatsInParallel(List<PaymentNotificationEntity> payments, 
                                                              LocalDate startDate, LocalDate endDate, 
                                                              Long adminId) {
        log.info("🚀 Iniciando cálculos paralelos para " + payments.size() + " pagos");
        
        return Uni.combine()
            .all()
            .unis(
                calculateBasicStats(payments),
                calculatePerformanceMetrics(payments),
                calculateDailySalesData(payments, startDate, endDate),
                calculateHourlySalesData(payments),
                calculateWeeklySalesData(payments, startDate, endDate),
                calculateMonthlySalesData(payments, startDate, endDate),
                calculateTopSellersData(payments, adminId)
            )
            .with((basicStats, performanceMetrics, dailySales, hourlySales, weeklySales, monthlySales, topSellers) -> {
                log.info("✅ Todos los cálculos paralelos completados exitosamente");
                return new ParallelStatsResult(basicStats, performanceMetrics, dailySales, hourlySales, weeklySales, monthlySales, topSellers);
            })
            .onFailure().invoke(throwable -> log.error("❌ Error en cálculos paralelos: " + throwable.getMessage()));
    }

    // Métodos auxiliares - removido calculateTotalSales ya que se usa directamente en los métodos

    // Clases de datos auxiliares
    public record BasicStats(double totalSales, long totalTransactions, double averageTransactionValue) {}
    
    public record PerformanceMetrics(double averageConfirmationTime, double claimRate, double rejectionRate,
                                   int pendingPayments, int confirmedPayments, int rejectedPayments) {}
    
    public record ParallelStatsResult(BasicStats basicStats, PerformanceMetrics performanceMetrics,
                                    List<DailySalesData> dailySales, List<HourlySalesData> hourlySales,
                                    List<WeeklySalesData> weeklySales, List<MonthlySalesData> monthlySales,
                                    List<TopSellerData> topSellers) {}
}
