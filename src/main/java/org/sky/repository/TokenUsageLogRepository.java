package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.TokenUsageLogEntity;

import java.util.List;

@ApplicationScoped
public class TokenUsageLogRepository implements PanacheRepository<TokenUsageLogEntity> {

    public Uni<List<TokenUsageLogEntity>> findByAdminId(Long adminId) {
        return find("adminId = ?1 order by createdAt desc", adminId).list();
    }
}
