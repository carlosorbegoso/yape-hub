package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.AdminEntity;
import org.sky.model.BusinessType;

@ApplicationScoped
public class AdminRepository implements PanacheRepository<AdminEntity> {
    
    public Uni<AdminEntity> findByUserId(Long userId) {
        return find("user.id", userId).firstResult();
    }
    
    public Uni<AdminEntity> findByRuc(String ruc) {
        return find("ruc", ruc).firstResult();
    }

}
