package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.ManualPayment;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class ManualPaymentRepository implements PanacheRepository<ManualPayment> {

    public Uni<List<ManualPayment>> findByAdminId(Long adminId) {
        return find("adminId = ?1 order by createdAt desc", adminId).list();
    }

    public Uni<List<ManualPayment>> findByAdminId(Long adminId, LocalDate startDate, LocalDate endDate) {
        return find("adminId = ?1 and createdAt >= ?2 and createdAt <= ?3 order by createdAt desc", 
                adminId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59)).list();
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

    public Uni<Long> countPendingPayments(LocalDate startDate, LocalDate endDate) {
        return count("status = 'pending' and createdAt >= ?1 and createdAt <= ?2", 
                startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
    }

    public Uni<Long> countApprovedPayments(LocalDate startDate, LocalDate endDate) {
        return count("status = 'approved' and createdAt >= ?1 and createdAt <= ?2", 
                startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
    }

    public Uni<Long> countRejectedPayments(LocalDate startDate, LocalDate endDate) {
        return count("status = 'rejected' and createdAt >= ?1 and createdAt <= ?2", 
                startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
    }

}
