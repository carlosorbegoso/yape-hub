package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.ManualPayment;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class ManualPaymentRepository implements PanacheRepositoryBase<ManualPayment, Long> {

    public Uni<List<ManualPayment>> findByAdminId(Long adminId) {
        return find("adminId = ?1 order by createdAt desc", adminId).list();
    }

    public Uni<List<ManualPayment>> findByAdminId(Long adminId, LocalDate startDate, LocalDate endDate) {
        return find("adminId = ?1 and createdAt >= ?2 and createdAt <= ?3 order by createdAt desc", 
                adminId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59)).list();
    }

    public Uni<List<ManualPayment>> findPendingPayments() {
        return find("status = 'pending' order by createdAt asc").list();
    }

    public Uni<List<ManualPayment>> findPendingPayments(LocalDate startDate, LocalDate endDate) {
        return find("status = 'pending' and createdAt >= ?1 and createdAt <= ?2 order by createdAt asc", 
                startDate.atStartOfDay(), endDate.atTime(23, 59, 59)).list();
    }

    public Uni<List<ManualPayment>> findByStatus(String status) {
        return find("status = ?1 order by createdAt desc", status).list();
    }

    public Uni<List<ManualPayment>> findByStatus(String status, LocalDate startDate, LocalDate endDate) {
        return find("status = ?1 and createdAt >= ?2 and createdAt <= ?3 order by createdAt desc", 
                status, startDate.atStartOfDay(), endDate.atTime(23, 59, 59)).list();
    }

    public Uni<List<ManualPayment>> findByAdminIdAndStatus(Long adminId, String status) {
        return find("adminId = ?1 and status = ?2 order by createdAt desc", adminId, status).list();
    }

    public Uni<List<ManualPayment>> findByAdminIdAndStatus(Long adminId, String status, LocalDate startDate, LocalDate endDate) {
        return find("adminId = ?1 and status = ?2 and createdAt >= ?3 and createdAt <= ?4 order by createdAt desc", 
                adminId, status, startDate.atStartOfDay(), endDate.atTime(23, 59, 59)).list();
    }

    public Uni<List<ManualPayment>> findByPaymentCodeId(Long paymentCodeId) {
        return find("paymentCodeId = ?1 order by createdAt desc", paymentCodeId).list();
    }

    public Uni<List<ManualPayment>> findByPaymentCodeId(Long paymentCodeId, LocalDate startDate, LocalDate endDate) {
        return find("paymentCodeId = ?1 and createdAt >= ?2 and createdAt <= ?3 order by createdAt desc", 
                paymentCodeId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59)).list();
    }

    public Uni<List<ManualPayment>> findByAdminReviewerId(Long adminReviewerId) {
        return find("adminReviewerId = ?1 order by reviewedAt desc", adminReviewerId).list();
    }

    public Uni<List<ManualPayment>> findByAdminReviewerId(Long adminReviewerId, LocalDate startDate, LocalDate endDate) {
        return find("adminReviewerId = ?1 and reviewedAt >= ?2 and reviewedAt <= ?3 order by reviewedAt desc", 
                adminReviewerId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59)).list();
    }

    public Uni<Long> countPendingPayments() {
        return count("status = 'pending'");
    }

    public Uni<Long> countPendingPayments(LocalDate startDate, LocalDate endDate) {
        return count("status = 'pending' and createdAt >= ?1 and createdAt <= ?2", 
                startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
    }

    public Uni<Long> countApprovedPayments() {
        return count("status = 'approved'");
    }

    public Uni<Long> countApprovedPayments(LocalDate startDate, LocalDate endDate) {
        return count("status = 'approved' and createdAt >= ?1 and createdAt <= ?2", 
                startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
    }

    public Uni<Long> countRejectedPayments() {
        return count("status = 'rejected'");
    }

    public Uni<Long> countRejectedPayments(LocalDate startDate, LocalDate endDate) {
        return count("status = 'rejected' and createdAt >= ?1 and createdAt <= ?2", 
                startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
    }

    public Uni<ManualPayment> findPendingByPaymentCodeId(Long paymentCodeId) {
        return find("paymentCodeId = ?1 and status = 'pending'", paymentCodeId).firstResult();
    }
}
