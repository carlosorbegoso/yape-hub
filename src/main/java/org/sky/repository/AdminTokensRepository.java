package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.AdminTokens;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class AdminTokensRepository implements PanacheRepositoryBase<AdminTokens, Long> {

    public Uni<AdminTokens> findByAdminId(Long adminId) {
        return find("adminId = ?1", adminId).firstResult();
    }

    public Uni<AdminTokens> findOrCreateByAdminId(Long adminId) {
        return findByAdminId(adminId)
                .chain(tokens -> {
                    if (tokens == null) {
                        // Crear nuevo registro de tokens para el admin
                        AdminTokens newTokens = new AdminTokens();
                        newTokens.adminId = adminId;
                        newTokens.tokensAvailable = 100; // Tokens iniciales del plan gratuito
                        newTokens.tokensUsed = 0;
                        newTokens.tokensPurchased = 0;
                        newTokens.lastResetDate = LocalDate.now();
                        
                        return persist(newTokens);
                    }
                    return Uni.createFrom().item(tokens);
                });
    }

    public Uni<List<AdminTokens>> findAdminsWithLowTokens(int threshold) {
        return find("tokensAvailable < ?1", threshold).list();
    }

    public Uni<List<AdminTokens>> findAdminsNeedingReset(LocalDate resetDate) {
        return find("lastResetDate < ?1", resetDate).list();
    }

    public Uni<Long> countAdminsWithTokens() {
        return count("tokensAvailable > 0");
    }

    public Uni<Double> getTotalTokensAvailable() {
        return find("select sum(tokensAvailable) from AdminTokens").project(Double.class).firstResult()
                .map(result -> result != null ? result : 0.0);
    }
}
