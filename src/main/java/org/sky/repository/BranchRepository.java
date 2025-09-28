package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.Branch;

import java.util.List;

@ApplicationScoped
public class BranchRepository implements PanacheRepository<Branch> {
    
    public Uni<List<Branch>> findByAdminId(Long adminId) {
        return find("admin.id", adminId).list();
    }

}
