package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.PaymentHistoryEntity;

import java.util.List;

@ApplicationScoped
public class PaymentHistoryRepository implements PanacheRepository<PaymentHistoryEntity> {

    public Uni<List<PaymentHistoryEntity>> findByAdminId(Long adminId) {
        return find("adminId", adminId).list();
    }

}
