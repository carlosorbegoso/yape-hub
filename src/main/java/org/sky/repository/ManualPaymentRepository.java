package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.ManualPayment;

import java.util.List;

@ApplicationScoped
public class ManualPaymentRepository implements PanacheRepositoryBase<ManualPayment, Long> {

    public Uni<List<ManualPayment>> findByAdminId(Long adminId) {
        return find("adminId = ?1 order by createdAt desc", adminId).list();
    }

    public Uni<List<ManualPayment>> findPendingPayments() {
        return find("status = 'pending' order by createdAt asc").list();
    }

    public Uni<List<ManualPayment>> findByStatus(String status) {
        return find("status = ?1 order by createdAt desc", status).list();
    }

    public Uni<List<ManualPayment>> findByAdminIdAndStatus(Long adminId, String status) {
        return find("adminId = ?1 and status = ?2 order by createdAt desc", adminId, status).list();
    }

    public Uni<List<ManualPayment>> findByPaymentCodeId(Long paymentCodeId) {
        return find("paymentCodeId = ?1 order by createdAt desc", paymentCodeId).list();
    }

    public Uni<List<ManualPayment>> findByAdminReviewerId(Long adminReviewerId) {
        return find("adminReviewerId = ?1 order by reviewedAt desc", adminReviewerId).list();
    }

    public Uni<Long> countPendingPayments() {
        return count("status = 'pending'");
    }

    public Uni<Long> countApprovedPayments() {
        return count("status = 'approved'");
    }

    public Uni<Long> countRejectedPayments() {
        return count("status = 'rejected'");
    }

    public Uni<ManualPayment> findPendingByPaymentCodeId(Long paymentCodeId) {
        return find("paymentCodeId = ?1 and status = 'pending'", paymentCodeId).firstResult();
    }
}
