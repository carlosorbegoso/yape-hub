package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.PaymentNotification;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class PaymentNotificationRepository implements PanacheRepository<PaymentNotification> {
    
    /**
     * Find pending payments for seller with pagination
     */
    public Uni<List<PaymentNotification>> findPendingPaymentsForSeller(Long sellerId, int page, int size, LocalDate startDate, LocalDate endDate) {
        return find(
            "seller.id = ?1 AND status = 'PENDING' AND createdAt BETWEEN ?2 AND ?3 ORDER BY createdAt DESC",
            sellerId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59)
        ).page(page, size).list();
    }

    /**
     * Find all pending payments with pagination
     */
    public Uni<List<PaymentNotification>> findAllPendingPayments(int page, int size, LocalDate startDate, LocalDate endDate) {
        return find(
            "status = 'PENDING' AND createdAt BETWEEN ?1 AND ?2 ORDER BY createdAt DESC",
            startDate.atStartOfDay(), endDate.atTime(23, 59, 59)
        ).page(page, size).list();
    }

    /**
     * Count pending payments for seller
     */
    public Uni<Long> countPendingPaymentsForSeller(Long sellerId, LocalDate startDate, LocalDate endDate) {
        return count(
            "seller.id = ?1 AND status = 'PENDING' AND createdAt BETWEEN ?2 AND ?3",
            sellerId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59)
        );
    }

    /**
     * Count all pending payments
     */
    public Uni<Long> countAllPendingPayments(LocalDate startDate, LocalDate endDate) {
        return count(
            "status = 'PENDING' AND createdAt BETWEEN ?1 AND ?2",
            startDate.atStartOfDay(), endDate.atTime(23, 59, 59)
        );
    }

    /**
     * Update payment status
     */
    public Uni<PaymentNotification> updatePaymentStatus(Long paymentId, String status) {
        return findById(paymentId)
            .chain(payment -> {
                if (payment != null) {
                    payment.status = status;
                    return persist(payment);
                }
                return Uni.createFrom().nullItem();
            });
    }
}
