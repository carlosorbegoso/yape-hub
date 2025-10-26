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
            log.info("🔄 TopSellersStrategy: Calculando top sellers para " + payments.size() + " pagos");
            
            // DEBUG: Analizar los pagos recibidos
            Map<String, Long> statusCount = payments.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    p -> p.status != null ? p.status : "NULL", 
                    java.util.stream.Collectors.counting()
                ));
            log.info("🔍 TopSellers - Status de pagos: " + statusCount);
            
            // DEBUG: Contar cuántos tienen confirmedBy
            long paymentsWithConfirmedBy = payments.stream()
                .filter(p -> "CLAIMED".equals(p.status))
                .filter(p -> p.confirmedBy != null)
                .count();
            log.info("🔍 TopSellers - Pagos CLAIMED con confirmedBy: " + paymentsWithConfirmedBy);
            
            Map<Long, Double> sellerSales = new ConcurrentHashMap<>();
            Map<Long, Long> sellerTransactions = new ConcurrentHashMap<>();
            
            processPayments(payments, adminId, sellerSales, sellerTransactions);
            List<TopSellerData> result = generateTopSellersData(sellerSales, sellerTransactions);
            
            log.info("✅ TopSellersStrategy: " + result.size() + " top sellers calculados");
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
        
        // DEBUG: Contar pagos válidos antes del procesamiento
        long validPayments = payments.stream()
            .filter(payment -> isValidPayment(payment, adminId))
            .count();
        log.info("🔍 TopSellers - Pagos válidos para procesar: " + validPayments + " de " + payments.size());
        
        if (validPayments == 0) {
            log.warn("⚠️ No hay pagos válidos (CLAIMED) para procesar en TopSellers");
            return;
        }
        
        var stream = payments.size() > PARALLEL_THRESHOLD ? 
            payments.parallelStream() : payments.stream();
            
        stream.filter(payment -> isValidPayment(payment, adminId))
              .forEach(payment -> {
                  try {
                      // Estrategia mejorada para identificar sellers:
                      // 1. Si confirmedBy existe, usarlo
                      // 2. Si no, usar adminId (el admin es el seller principal)
                      Long sellerId = payment.confirmedBy != null ? payment.confirmedBy : adminId;
                      
                      double amount = getValidAmount(payment.amount);
                      sellerSales.merge(sellerId, amount, Double::sum);
                      sellerTransactions.merge(sellerId, 1L, Long::sum);
                      
                      log.info("💰 Procesado: Seller " + sellerId + " += $" + amount + 
                              " (confirmedBy: " + payment.confirmedBy + ", paymentId: " + payment.id + ")");
                  } catch (Exception e) {
                      log.warn("⚠️ Error procesando pago ID " + payment.id + ": " + e.getMessage());
                  }
              });
        
        log.info("🔍 TopSellers procesados: " + sellerSales.size() + " sellers únicos encontrados");
        if (sellerSales.isEmpty()) {
            log.error("❌ PROBLEMA: No se procesaron sellers a pesar de tener " + validPayments + " pagos válidos");
        } else {
            sellerSales.forEach((sellerId, sales) -> 
                log.info("  ✅ Seller " + sellerId + ": $" + sales + " (" + sellerTransactions.get(sellerId) + " transacciones)")
            );
        }
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
            log.error("❌ CRÍTICO: No hay datos de sellers para generar top sellers - esto no debería pasar si hay pagos CLAIMED");
            return List.of();
        }
        
        log.info("🔄 Generando TopSellers con " + sellerSales.size() + " sellers:");
        sellerSales.forEach((id, sales) -> 
            log.info("  - Seller " + id + ": $" + sales + " (" + sellerTransactions.get(id) + " transacciones)")
        );
        
        List<TopSellerData> result = sellerSales.entrySet().stream()
            .filter(entry -> entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0)
            .map(entry -> {
                Long sellerId = entry.getKey();
                Double sales = entry.getValue();
                Long transactions = sellerTransactions.getOrDefault(sellerId, 0L);
                
                // Generar nombres más descriptivos
                String sellerName = sellerId.equals(1L) ? "Administrador Principal" : "Usuario " + sellerId;
                String branchName = "Sucursal " + sellerId;
                
                TopSellerData seller = new TopSellerData(
                    0, // rank se asignará después
                    sellerId,
                    sellerName,
                    branchName,
                    Math.round(sales * 100.0) / 100.0, // Redondear a 2 decimales
                    transactions
                );
                
                log.info("📊 Creado TopSeller: " + seller);
                return seller;
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
        
        log.info("✅ TopSellers generados exitosamente: " + result.size() + " sellers en el ranking");
        result.forEach(seller -> log.info("  🏆 #" + seller.rank() + " " + seller.sellerName() + ": $" + seller.totalSales()));
        
        return result;
    }
}
