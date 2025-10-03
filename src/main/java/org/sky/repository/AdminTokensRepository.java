package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.AdminTokensEntity;

import java.time.LocalDate;

@ApplicationScoped
public class AdminTokensRepository implements PanacheRepository<AdminTokensEntity> {

    public Uni<AdminTokensEntity> findByAdminId(Long adminId) {
        return find("adminId = ?1", adminId).firstResult();
    }

    public Uni<AdminTokensEntity> findOrCreateByAdminId(Long adminId) {
        return findByAdminId(adminId)
                .chain(tokens -> {
                    if (tokens == null) {
                        AdminTokensEntity newTokens = new AdminTokensEntity();
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
