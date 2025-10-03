package org.sky.service.auth;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.auth.LoginRequest;
import org.sky.model.SellerEntity;
import org.sky.model.UserEntityEntity;
import org.sky.repository.SellerRepository;
import org.sky.repository.UserRepository;

import java.time.LocalDateTime;

@ApplicationScoped
public class UserLoginService {

    @Inject
    UserRepository userRepository;
    
    @Inject
    SellerRepository sellerRepository;

    public Uni<UserEntityEntity> getUserForLogin(LoginRequest request) {
        return userRepository.findByEmailAndRoleForLogin(request.email(), request.role());
    }

    public Uni<UserEntityEntity> updateUserLoginInfo(UserEntityEntity user, LoginRequest request) {
        user.lastLogin = LocalDateTime.now();
        user.deviceFingerprint = request.deviceFingerprint();
        return userRepository.persist(user);
    }
    
    public Uni<SellerEntity> getSellerForUser(UserEntityEntity user) {
        return sellerRepository.findByUserId(user.id);
    }

}
