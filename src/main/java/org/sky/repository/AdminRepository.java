package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.Admin;

import java.util.List;

@ApplicationScoped
public class AdminRepository implements PanacheRepositoryBase<Admin, Long> {
    
    public Uni<Admin> findByUserId(Long userId) {
        return find("user.id", userId).firstResult();
    }
    
    public Uni<Admin> findByRuc(String ruc) {
        return find("ruc", ruc).firstResult();
    }
    
    public Uni<List<Admin>> findByBusinessType(Admin.BusinessType businessType) {
        return find("businessType", businessType).list();
    }
    
    public Uni<List<Admin>> findActiveAdmins() {
        return find("user.isActive = true").list();
    }
}
