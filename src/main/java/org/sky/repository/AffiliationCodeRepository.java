package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.AffiliationCodeEntity;

@ApplicationScoped
public class AffiliationCodeRepository implements PanacheRepository<AffiliationCodeEntity> {
    
    public Uni<AffiliationCodeEntity> findByAffiliationCode(String affiliationCode) {
        return find("SELECT a FROM AffiliationCodeEntity a JOIN FETCH a.branch b JOIN FETCH b.admin WHERE a.affiliationCode = ?1", affiliationCode).firstResult();
    }

}
