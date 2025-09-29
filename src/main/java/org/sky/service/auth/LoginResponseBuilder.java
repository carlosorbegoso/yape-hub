package org.sky.service.auth;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.ApiResponse;
import org.sky.dto.auth.LoginResponse;
import org.sky.model.User;
import org.sky.repository.AdminRepository;
import org.sky.repository.SellerRepository;

@ApplicationScoped
public class LoginResponseBuilder {

    @Inject
    AdminRepository adminRepository;
    
    @Inject
    SellerRepository sellerRepository;

    public Uni<ApiResponse<LoginResponse>> buildLoginResponse(JwtTokenService.TokenData tokenData) {
        User user = tokenData.user();
        
        if (user.role == User.UserRole.ADMIN) {
            return buildAdminLoginResponse(tokenData);
        } else {
            return buildSellerLoginResponse(tokenData);
        }
    }

    public Uni<ApiResponse<LoginResponse>> buildLoginResponseFromCachedUser(JwtTokenService.TokenData tokenData) {
        User user = tokenData.user();
        
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
            user.id, user.email, user.role,
            null, null, user.isVerified, null
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
                    
                    LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                        tokenData.user().id, tokenData.user().email, tokenData.user().role,
                        businessId, businessName, tokenData.user().isVerified, null
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
                    
                    LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                        tokenData.user().id, tokenData.user().email, tokenData.user().role,
                        businessId, businessName, tokenData.user().isVerified, sellerId
                    );
                    
                    LoginResponse response = new LoginResponse(
                        tokenData.accessToken(), tokenData.refreshToken(), 3600L, userInfo
                    );
                    
                    return ApiResponse.success("Login exitoso", response);
                });
    }

}
