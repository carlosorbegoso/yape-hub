package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.User;

import java.util.List;

@ApplicationScoped
public class UserRepository implements PanacheRepositoryBase<User, Long> {
    
    public Uni<User> findByEmail(String email) {
        return find("email", email).firstResult();
    }
    
    public Uni<User> findByEmailAndRole(String email, User.UserRole role) {
        return find("email = ?1 and role = ?2", email, role).firstResult();
    }
    

}
