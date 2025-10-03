package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.PaymentCodeEntity;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class PaymentCodeRepository implements PanacheRepository<PaymentCodeEntity> {

    public Uni<PaymentCodeEntity> findByCode(String code) {
        return find("code = ?1", code).firstResult();
    }

    public Uni<List<PaymentCodeEntity>> findByAdminId(Long adminId) {
        return find("adminId = ?1 order by createdAt desc", adminId).list();
    }


    public Uni<List<PaymentCodeEntity>> findByStatus(String status) {
        return find("status = ?1 order by createdAt desc", status).list();
    }

    public Uni<Long> countPendingCodes() {
        return count("status = 'pending' and expiresAt > ?1", LocalDateTime.now());
    }

    public Uni<PaymentCodeEntity> findValidCode(String code) {
        return find("code = ?1 and status = 'pending' and expiresAt > ?2", code, LocalDateTime.now()).firstResult();
    }
}
