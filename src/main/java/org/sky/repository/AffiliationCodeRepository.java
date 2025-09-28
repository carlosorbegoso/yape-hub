package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.AffiliationCode;

@ApplicationScoped
public class AffiliationCodeRepository implements PanacheRepository<AffiliationCode> {
    
    public Uni<AffiliationCode> findByAffiliationCode(String affiliationCode) {
        return find("affiliationCode", affiliationCode).firstResult();
    }

}
