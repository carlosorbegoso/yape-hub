package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.PaymentHistory;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class PaymentHistoryRepository implements PanacheRepository<PaymentHistory> {

    public Uni<List<PaymentHistory>> findByAdminId(Long adminId) {
        return find("adminId", adminId).list();
    }

}
