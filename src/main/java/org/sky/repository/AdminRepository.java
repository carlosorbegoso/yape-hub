package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.AdminEntity;

@ApplicationScoped
public class AdminRepository implements PanacheRepository<AdminEntity> {
    
    public Uni<AdminEntity> findByUserId(Long userId) {
        return find("user.id", userId).firstResult();
    }
    
    public Uni<AdminEntity> findByRuc(String ruc) {
        return find("ruc", ruc).firstResult();
    }
    
    public Uni<java.util.List<AdminEntity>> findAllWithLimit(int limit) {
        return findAll().range(0, Math.min(limit, 1000)).list(); // MAX 1000
    }
    
    public Uni<java.util.List<AdminEntity>> findActiveAdmins() {
        return find("user.isActive = true").range(0, 500).list(); // LIMIT 500
    }
    
    /**
     * Find admin by ID ensuring the user relation is properly loaded
     * This method helps avoid NPE issues when accessing admin.user properties
     */
    public Uni<AdminEntity> findByIdWithUser(Long adminId) {
        return find("SELECT a FROM AdminEntity a JOIN FETCH a.user WHERE a.id = ?1", adminId).firstResult();
    }

}
