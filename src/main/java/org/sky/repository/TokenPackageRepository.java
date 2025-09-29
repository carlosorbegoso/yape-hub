package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import org.sky.model.TokenPackage;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class TokenPackageRepository implements PanacheRepository<TokenPackage> {

    public Uni<List<TokenPackage>> findActivePackages() {
        return find("isActive = true order by sortOrder asc, tokens asc").list();
    }
    
    public Uni<TokenPackage> findByPackageId(String packageId) {
        return find("packageId = ?1 and isActive = true", packageId).firstResult();
    }
    



}
