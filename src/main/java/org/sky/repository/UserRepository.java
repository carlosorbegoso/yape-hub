package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.UserEntityEntity;
import org.sky.model.UserRole;

@ApplicationScoped
public class UserRepository implements PanacheRepositoryBase<UserEntityEntity, Long> {
    
    public Uni<UserEntityEntity> findByEmail(String email) {
        return find("email", email).firstResult();
    }
    
    public Uni<UserEntityEntity> findByEmailAndRole(String email, UserRole role) {
        return find("email = ?1 and role = ?2", email, role).firstResult();
    }
    
    // Consulta para login
    public Uni<UserEntityEntity> findByEmailAndRoleForLogin(String email, UserRole role) {
        return find("email = ?1 and role = ?2", email, role).firstResult();
    }
    
    // Consulta para refreshToken
    public Uni<UserEntityEntity> findByIdForRefresh(Long userId) {
        return findById(userId);
    }

}
