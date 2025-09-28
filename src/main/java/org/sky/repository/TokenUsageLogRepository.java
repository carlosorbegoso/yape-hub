package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.TokenUsageLog;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class TokenUsageLogRepository implements PanacheRepository<TokenUsageLog> {

    public Uni<List<TokenUsageLog>> findByAdminId(Long adminId) {
        return find("adminId = ?1 order by createdAt desc", adminId).list();
    }
}
