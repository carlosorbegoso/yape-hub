package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.TokenUsageLog;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class TokenUsageLogRepository implements PanacheRepositoryBase<TokenUsageLog, Long> {

    public Uni<List<TokenUsageLog>> findByAdminId(Long adminId) {
        return find("adminId = ?1 order by createdAt desc", adminId).list();
    }

    public Uni<List<TokenUsageLog>> findByAdminIdAndPeriod(Long adminId, LocalDateTime startDate, LocalDateTime endDate) {
        return find("adminId = ?1 and createdAt >= ?2 and createdAt <= ?3 order by createdAt desc", 
                   adminId, startDate, endDate).list();
    }

    public Uni<List<TokenUsageLog>> findByOperationType(String operationType) {
        return find("operationType = ?1 order by createdAt desc", operationType).list();
    }

    public Uni<List<TokenUsageLog>> findByAdminIdAndOperationType(Long adminId, String operationType) {
        return find("adminId = ?1 and operationType = ?2 order by createdAt desc", adminId, operationType).list();
    }

    public Uni<Long> countTokensUsedByAdmin(Long adminId) {
        return find("select sum(tokensConsumed) from TokenUsageLog where adminId = ?1", adminId)
                .project(Long.class).firstResult()
                .map(result -> result != null ? result : 0L);
    }

    public Uni<Long> countTokensUsedByAdminAndPeriod(Long adminId, LocalDateTime startDate, LocalDateTime endDate) {
        return find("select sum(tokensConsumed) from TokenUsageLog where adminId = ?1 and createdAt >= ?2 and createdAt <= ?3", 
                   adminId, startDate, endDate)
                .project(Long.class).firstResult()
                .map(result -> result != null ? result : 0L);
    }

    public Uni<List<TokenUsageLog>> findRecentUsage(Long adminId, int limit) {
        return find("adminId = ?1 order by createdAt desc", adminId)
                .page(0, limit)
                .list();
    }

    public Uni<List<TokenUsageLog>> findTopOperationsByAdmin(Long adminId) {
        return find("adminId = ?1 group by operationType order by sum(tokensConsumed) desc", adminId).list();
    }
}
