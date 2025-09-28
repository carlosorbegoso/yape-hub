package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.Admin;

@ApplicationScoped
public class AdminRepository implements PanacheRepository<Admin> {
    
    public Uni<Admin> findByUserId(Long userId) {
        return find("user.id", userId).firstResult();
    }
    
    public Uni<Admin> findByRuc(String ruc) {
        return find("ruc", ruc).firstResult();
    }

}
