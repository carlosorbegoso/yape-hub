package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.AffiliationCode;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class AffiliationCodeRepository implements PanacheRepositoryBase<AffiliationCode, Long> {
    
    public Uni<AffiliationCode> findByAffiliationCode(String affiliationCode) {
        return find("affiliationCode", affiliationCode).firstResult();
    }

}
