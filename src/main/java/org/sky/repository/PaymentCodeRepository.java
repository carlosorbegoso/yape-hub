package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.PaymentCode;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class PaymentCodeRepository implements PanacheRepositoryBase<PaymentCode, Long> {

    public Uni<PaymentCode> findByCode(String code) {
        return find("code = ?1", code).firstResult();
    }

    public Uni<List<PaymentCode>> findByAdminId(Long adminId) {
        return find("adminId = ?1 order by createdAt desc", adminId).list();
    }

    public Uni<List<PaymentCode>> findPendingCodes() {
        return find("status = 'pending' and expiresAt > ?1 order by createdAt desc", LocalDateTime.now()).list();
    }

    public Uni<List<PaymentCode>> findExpiredCodes() {
        return find("status = 'pending' and expiresAt < ?1", LocalDateTime.now()).list();
    }

    public Uni<List<PaymentCode>> findByStatus(String status) {
        return find("status = ?1 order by createdAt desc", status).list();
    }

    public Uni<List<PaymentCode>> findByAdminIdAndStatus(Long adminId, String status) {
        return find("adminId = ?1 and status = ?2 order by createdAt desc", adminId, status).list();
    }

    public Uni<Long> countPendingCodes() {
        return count("status = 'pending' and expiresAt > ?1", LocalDateTime.now());
    }

    public Uni<Long> countExpiredCodes() {
        return count("status = 'pending' and expiresAt < ?1", LocalDateTime.now());
    }

    public Uni<PaymentCode> findValidCode(String code) {
        return find("code = ?1 and status = 'pending' and expiresAt > ?2", code, LocalDateTime.now()).firstResult();
    }
}
