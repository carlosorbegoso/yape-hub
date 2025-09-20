package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import org.sky.model.TokenPackage;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class TokenPackageRepository implements PanacheRepository<TokenPackage> {
    
    /**
     * Obtiene todos los paquetes de tokens activos ordenados por sortOrder
     */
    public Uni<List<TokenPackage>> findActivePackages() {
        return find("isActive = true order by sortOrder asc, tokens asc").list();
    }
    
    /**
     * Obtiene un paquete por su packageId
     */
    public Uni<TokenPackage> findByPackageId(String packageId) {
        return find("packageId = ?1 and isActive = true", packageId).firstResult();
    }
    
    /**
     * Obtiene el paquete más popular
     */
    public Uni<TokenPackage> findPopularPackage() {
        return find("isPopular = true and isActive = true").firstResult();
    }
    
    /**
     * Obtiene paquetes por rango de tokens
     */
    public Uni<List<TokenPackage>> findByTokenRange(Integer minTokens, Integer maxTokens) {
        return find("tokens >= ?1 and tokens <= ?2 and isActive = true order by tokens asc", 
                   minTokens, maxTokens).list();
    }
    
    /**
     * Obtiene paquetes con descuento
     */
    public Uni<List<TokenPackage>> findPackagesWithDiscount() {
        return find("discount > 0 and isActive = true order by discount desc").list();
    }
    
    /**
     * Verifica si existe un paquete con el packageId dado
     */
    public Uni<Boolean> existsByPackageId(String packageId) {
        return count("packageId = ?1", packageId).map(count -> count > 0);
    }
}
