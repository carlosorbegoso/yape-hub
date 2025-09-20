package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.PaymentHistory;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class PaymentHistoryRepository implements PanacheRepositoryBase<PaymentHistory, Long> {

    public Uni<List<PaymentHistory>> findByAdminId(Long adminId) {
        return find("adminId", adminId).list();
    }

    public Uni<List<PaymentHistory>> findByAdminIdAndPeriod(Long adminId, LocalDateTime startDate, LocalDateTime endDate) {
        return find("adminId = ?1 and createdAt >= ?2 and createdAt <= ?3 order by createdAt desc", 
                   adminId, startDate, endDate).list();
    }

    public Uni<List<PaymentHistory>> findByStatus(String status) {
        return find("status = ?1 order by createdAt desc", status).list();
    }

    public Uni<List<PaymentHistory>> findByPaymentType(String paymentType) {
        return find("paymentType = ?1 order by createdAt desc", paymentType).list();
    }

    public Uni<List<PaymentHistory>> findByPaymentMethod(String paymentMethod) {
        return find("paymentMethod = ?1 order by createdAt desc", paymentMethod).list();
    }

    public Uni<List<PaymentHistory>> findByAdminIdAndStatus(Long adminId, String status) {
        return find("adminId = ?1 and status = ?2 order by createdAt desc", adminId, status).list();
    }

    public Uni<List<PaymentHistory>> findCompletedPayments() {
        return find("status = 'completed' order by createdAt desc").list();
    }

    public Uni<List<PaymentHistory>> findPendingPayments() {
        return find("status = 'pending' order by createdAt asc").list();
    }

    public Uni<Double> getTotalRevenueByPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        return find("select sum(amountPen) from PaymentHistory where status = 'completed' and createdAt >= ?1 and createdAt <= ?2", 
                   startDate, endDate)
                .project(Double.class).firstResult()
                .map(result -> result != null ? result : 0.0);
    }

    public Uni<Double> getTotalRevenueByAdmin(Long adminId) {
        return find("select sum(amountPen) from PaymentHistory where adminId = ?1 and status = 'completed'", adminId)
                .project(Double.class).firstResult()
                .map(result -> result != null ? result : 0.0);
    }

    public Uni<Long> countPaymentsByAdmin(Long adminId) {
        return count("adminId = ?1", adminId);
    }

    public Uni<Long> countCompletedPaymentsByAdmin(Long adminId) {
        return count("adminId = ?1 and status = 'completed'", adminId);
    }
}
