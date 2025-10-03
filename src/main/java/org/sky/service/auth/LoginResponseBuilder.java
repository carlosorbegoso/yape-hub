package org.sky.service.auth;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.sky.dto.response.ApiResponse;
import org.sky.dto.response.auth.LoginResponse;
import org.sky.dto.response.common.UserInfo;
import org.sky.model.UserEntityEntity;
import org.sky.model.UserRole;
import org.sky.repository.AdminRepository;
import org.sky.repository.SellerRepository;

@ApplicationScoped
public class LoginResponseBuilder {

    @Inject
    AdminRepository adminRepository;
    
    @Inject
    SellerRepository sellerRepository;

    public Uni<ApiResponse<LoginResponse>> buildLoginResponse(JwtTokenService.TokenData tokenData) {
        UserEntityEntity user = tokenData.user();
        
        if (user.role == UserRole.ADMIN) {
            return buildAdminLoginResponse(tokenData);
        } else {
            return buildSellerLoginResponse(tokenData);
        }
    }

    public Uni<ApiResponse<LoginResponse>> buildLoginResponseFromCachedUser(JwtTokenService.TokenData tokenData) {
        UserEntityEntity user = tokenData.user();
        
        // Para usuarios en caché, necesitamos obtener la información del business
        if (user.role == UserRole.ADMIN) {
            return buildAdminLoginResponse(tokenData);
        } else {
            return buildSellerLoginResponse(tokenData);
        }
    }

    private Uni<ApiResponse<LoginResponse>> buildAdminLoginResponse(JwtTokenService.TokenData tokenData) {
        return adminRepository.findByUserId(tokenData.user().id)
                .map(UserInfo::fromAdmin)
                .map(userInfo -> ApiResponse.success("Login exitoso", 
                    LoginResponse.create(tokenData.accessToken(), tokenData.refreshToken(), userInfo)));
    }

    private Uni<ApiResponse<LoginResponse>> buildSellerLoginResponse(JwtTokenService.TokenData tokenData) {
        return sellerRepository.findByUserId(tokenData.user().id)
                .map(UserInfo::fromSeller)
                .map(userInfo -> ApiResponse.success("Login exitoso", 
                    LoginResponse.create(tokenData.accessToken(), tokenData.refreshToken(), userInfo)));
    }

}
