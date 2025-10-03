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
        
        UserInfo userInfo = new UserInfo(
            user.id, user.email, null, user.role.toString(), user.isVerified
        );
        
        LoginResponse response = new LoginResponse(
            tokenData.accessToken(), tokenData.refreshToken(), 3600L, userInfo
        );
        
        return Uni.createFrom().item(ApiResponse.success("Login exitoso", response));
    }

    private Uni<ApiResponse<LoginResponse>> buildAdminLoginResponse(JwtTokenService.TokenData tokenData) {
        return adminRepository.findByUserId(tokenData.user().id)
                .map(admin -> {
                    Long businessId = admin != null ? admin.id : null;
                    String businessName = admin != null ? admin.businessName : null;
                    
                    UserInfo userInfo = new UserInfo(
                        tokenData.user().id, tokenData.user().email, businessName, tokenData.user().role.toString(), tokenData.user().isVerified
                    );
                    
                    LoginResponse response = new LoginResponse(
                        tokenData.accessToken(), tokenData.refreshToken(), 3600L, userInfo
                    );
                    
                    return ApiResponse.success("Login exitoso", response);
                });
    }

    private Uni<ApiResponse<LoginResponse>> buildSellerLoginResponse(JwtTokenService.TokenData tokenData) {
        return sellerRepository.findByUserId(tokenData.user().id)
                .map(seller -> {
                    Long businessId = null;
                    String businessName = null;
                    Long sellerId = seller != null ? seller.id : null;
                    
                    if (seller != null && seller.branch != null && seller.branch.admin != null) {
                        businessId = seller.branch.admin.id;
                        businessName = seller.branch.admin.businessName;
                    }
                    
                    UserInfo userInfo = new UserInfo(
                        tokenData.user().id, tokenData.user().email, businessName, tokenData.user().role.toString(), tokenData.user().isVerified
                    );
                    
                    LoginResponse response = new LoginResponse(
                        tokenData.accessToken(), tokenData.refreshToken(), 3600L, userInfo
                    );
                    
                    return ApiResponse.success("Login exitoso", response);
                });
    }

}
