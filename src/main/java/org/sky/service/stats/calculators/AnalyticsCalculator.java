package org.sky.service.stats.calculators;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.sky.dto.stats.AnalyticsSummaryResponse;
import org.sky.dto.stats.SellerAnalyticsResponse;
import org.sky.model.PaymentNotification;
import org.sky.model.Seller;
import org.sky.repository.PaymentNotificationRepository;
import org.sky.repository.SellerRepository;
import org.sky.service.stats.services.AnalyticsDataProcessor;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

import java.time.LocalDate;

@ApplicationScoped
public class AnalyticsCalculator {
    
    @Inject
    PaymentNotificationRepository paymentNotificationRepository;
    
    @Inject
    SellerRepository sellerRepository;
    
    @Inject
    AnalyticsDataProcessor analyticsDataProcessor;
    
    private static final Logger log = Logger.getLogger(AnalyticsCalculator.class);
    
    @WithTransaction
    public Uni<AnalyticsSummaryResponse> calculateAdminAnalyticsSummary(Long adminId, LocalDate startDate, LocalDate endDate, 
                                                                      String include, String period, String metric, 
                                                                      String granularity, Double confidence, Integer days) {
        log.info("📊 AnalyticsCalculator.calculateAdminAnalyticsSummary() - AdminId: " + adminId + 
                ", Desde: " + startDate + ", Hasta: " + endDate);
        
        return paymentNotificationRepository.findPaymentsForAnalytics(
                adminId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
                .chain(payments -> {
                    // OPTIMIZADO: Solo obtener sellers activos con límite
                    return sellerRepository.find("branch.admin.id = ?1 and isActive = true", adminId)
                            .page(0, 100)  // LÍMITE: máximo 100 sellers
                            .list()
                            .map(sellers -> {
                                log.info("📊 Procesando analytics para " + payments.size() + " pagos y " + sellers.size() + " vendedores");
                                
                                return analyticsDataProcessor.processAdminAnalytics(payments, sellers, startDate, endDate);
                            });
                });
    }
    
    @WithTransaction
    public Uni<SellerAnalyticsResponse> calculateSellerAnalyticsSummary(Long sellerId, LocalDate startDate, LocalDate endDate,
                                                                        String include, String period, String metric,
                                                                        String granularity, Double confidence, Integer days) {
        log.info("📊 AnalyticsCalculator.calculateSellerAnalyticsSummary() - SellerId: " + sellerId + 
                ", Desde: " + startDate + ", Hasta: " + endDate);
        
        return sellerRepository.findById(sellerId)
                .chain(seller -> {
                    if (seller == null) {
                        log.warn("❌ Vendedor no encontrado con ID: " + sellerId);
                        return Uni.createFrom().failure(org.sky.exception.ResourceNotFoundException.byField("Vendedor", "id", sellerId));
                    }
                    
                    if (seller.branch == null || seller.branch.admin == null) {
                        log.warn("❌ Vendedor sin sucursal o admin válido: " + sellerId);
                        return Uni.createFrom().failure(org.sky.exception.ResourceNotFoundException.byField("Vendedor", "configuración", sellerId));
                    }
                    
                    // Obtener todos los pagos del admin del vendedor
                    return paymentNotificationRepository.find("adminId = ?1 and createdAt >= ?2 and createdAt <= ?3", 
                            seller.branch.admin.id, startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
                            .list()
                            .map(payments -> {
                                log.info("📊 Procesando analytics para vendedor " + seller.sellerName + " con " + payments.size() + " pagos del admin");
                                
                                return analyticsDataProcessor.processSellerAnalytics(payments, sellerId, startDate, endDate);
                            });
                });
    }
}
