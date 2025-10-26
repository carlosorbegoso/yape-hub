package org.sky.service.stats.calculators.strategy;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.sky.dto.response.stats.TopSellerData;
import org.sky.model.PaymentNotificationEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Strategy Pattern: Implementación específica para datos de top sellers
 * Clean Code: Responsabilidad única - solo calcula top sellers
 */
@ApplicationScoped
public class TopSellersStrategy implements CalculationStrategy<List<TopSellerData>> {
    
    private static final Logger log = Logger.getLogger(TopSellersStrategy.class);
    private static final int PARALLEL_THRESHOLD = 1000;
    private static final int TOP_SELLERS_LIMIT = 10;
    
    @Override
    public Uni<List<TopSellerData>> calculate(List<PaymentNotificationEntity> payments, 
                                            LocalDate startDate, 
                                            LocalDate endDate, 
                                            Long adminId) {
        return Uni.createFrom().item(() -> {
            log.debug("🔄 TopSellersStrategy: Calculando top sellers para " + payments.size() + " pagos");
            
            Map<Long, Double> sellerSales = new ConcurrentHashMap<>();
            Map<Long, Long> sellerTransactions = new ConcurrentHashMap<>();
            
            processPayments(payments, adminId, sellerSales, sellerTransactions);
            List<TopSellerData> result = generateTopSellersData(sellerSales, sellerTransactions);
            
            log.debug("✅ TopSellersStrategy: " + result.size() + " top sellers calculados");
            return result;
        });
    }
    
    @Override
    public boolean canHandle(List<PaymentNotificationEntity> payments, 
                           LocalDate startDate, 
                           LocalDate endDate, 
                           Long adminId) {
        return payments != null && !payments.isEmpty();
    }
    
    @Override
    public String getStrategyName() {
        return "TopSellersStrategy";
    }
    
    private void processPayments(List<PaymentNotificationEntity> payments,
                               Long adminId,
                               Map<Long, Double> sellerSales,
                               Map<Long, Long> sellerTransactions) {
        
        var stream = payments.size() > PARALLEL_THRESHOLD ? 
            payments.parallelStream() : payments.stream();
            
        stream.filter(payment -> isValidPayment(payment, adminId))
              .forEach(payment -> {
                  try {
                      // Para top sellers, usar confirmedBy como identificador del seller
                      // Si confirmedBy es null, usar adminId como fallback
                      Long sellerId = payment.confirmedBy != null ? payment.confirmedBy : adminId;
                      if (sellerId != null) {
                          double amount = getValidAmount(payment.amount);
                          sellerSales.merge(sellerId, amount, Double::sum);
                          sellerTransactions.merge(sellerId, 1L, Long::sum);
                      }
                  } catch (Exception e) {
                      log.warn("⚠️ Error procesando pago ID " + payment.id + ": " + e.getMessage());
                  }
              });
        
        log.info("🔍 TopSellers procesados: " + sellerSales.size() + " sellers únicos encontrados");
        sellerSales.forEach((sellerId, sales) -> 
            log.debug("  Seller " + sellerId + ": $" + sales + " (" + sellerTransactions.get(sellerId) + " transacciones)")
        );
    }
    
    private boolean isValidPayment(PaymentNotificationEntity payment, Long adminId) {
        return payment.amount != null && 
               payment.amount > 0 &&
               "CLAIMED".equals(payment.status);
    }
    
    private double getValidAmount(Double amount) {
        return (amount != null && amount > 0) ? amount : 0.0;
    }
    
    private List<TopSellerData> generateTopSellersData(Map<Long, Double> sellerSales,
                                                     Map<Long, Long> sellerTransactions) {
        
        if (sellerSales.isEmpty()) {
            log.info("📊 No hay datos de sellers para generar top sellers");
            return List.of();
        }
        
        List<TopSellerData> result = sellerSales.entrySet().parallelStream()
            .filter(entry -> entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0)
            .map(entry -> {
                Long sellerId = entry.getKey();
                Double sales = entry.getValue();
                Long transactions = sellerTransactions.getOrDefault(sellerId, 0L);
                return new TopSellerData(
                    0, // rank se asignará después
                    sellerId,
                    "Usuario " + sellerId, // Cambiar nombre para ser más descriptivo
                    "Sucursal " + sellerId,
                    Math.round(sales * 100.0) / 100.0, // Redondear a 2 decimales
                    transactions
                );
            })
            .sorted((a, b) -> Double.compare(b.totalSales(), a.totalSales()))
            .limit(TOP_SELLERS_LIMIT)
            .collect(java.util.stream.Collectors.toList());
        
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
        
        log.info("✅ TopSellers generados: " + result.size() + " sellers en el ranking");
        return result;
    }
}
