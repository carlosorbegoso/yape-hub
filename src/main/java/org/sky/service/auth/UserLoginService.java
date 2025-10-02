package org.sky.service.auth;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.auth.LoginRequest;
import org.sky.model.SellerEntity;
import org.sky.model.UserEntity;
import org.sky.repository.SellerRepository;
import org.sky.repository.UserRepository;

import java.time.LocalDateTime;

@ApplicationScoped
public class UserLoginService {

    @Inject
    UserRepository userRepository;
    
    @Inject
    SellerRepository sellerRepository;

    public Uni<UserEntity> getUserForLogin(LoginRequest request) {
        return userRepository.findByEmailAndRoleForLogin(request.email(), request.role());
    }

    public Uni<UserEntity> updateUserLoginInfo(UserEntity user, LoginRequest request) {
        user.lastLogin = LocalDateTime.now();
        user.deviceFingerprint = request.deviceFingerprint();
        return userRepository.persist(user);
    }
    
    public Uni<SellerEntity> getSellerForUser(UserEntity user) {
        return sellerRepository.findByUserId(user.id);
    }

}
