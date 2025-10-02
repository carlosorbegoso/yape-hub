package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.UserEntity;
import org.sky.model.UserRole;

@ApplicationScoped
public class UserRepository implements PanacheRepositoryBase<UserEntity, Long> {
    
    public Uni<UserEntity> findByEmail(String email) {
        return find("email", email).firstResult();
    }
    
    public Uni<UserEntity> findByEmailAndRole(String email, UserRole role) {
        return find("email = ?1 and role = ?2", email, role).firstResult();
    }
    
    // Consulta para login
    public Uni<UserEntity> findByEmailAndRoleForLogin(String email, UserRole role) {
        return find("email = ?1 and role = ?2", email, role).firstResult();
    }
    
    // Consulta para refreshToken
    public Uni<UserEntity> findByIdForRefresh(Long userId) {
        return findById(userId);
    }

}
