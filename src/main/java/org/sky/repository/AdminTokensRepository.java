package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.AdminTokens;

import java.time.LocalDate;

@ApplicationScoped
public class AdminTokensRepository implements PanacheRepository<AdminTokens> {

    public Uni<AdminTokens> findByAdminId(Long adminId) {
        return find("adminId = ?1", adminId).firstResult();
    }

    public Uni<AdminTokens> findOrCreateByAdminId(Long adminId) {
        return findByAdminId(adminId)
                .chain(tokens -> {
                    if (tokens == null) {
                        AdminTokens newTokens = new AdminTokens();
                        newTokens.adminId = adminId;
                        newTokens.tokensAvailable = 100;
                        newTokens.tokensUsed = 0;
                        newTokens.tokensPurchased = 0;
                        newTokens.lastResetDate = LocalDate.now();
                        
                        return persist(newTokens);
                    }
                    return Uni.createFrom().item(tokens);
                });
    }

}
