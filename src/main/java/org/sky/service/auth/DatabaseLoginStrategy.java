package org.sky.service.auth;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.ApiResponse;
import org.sky.dto.auth.LoginRequest;
import org.sky.dto.auth.LoginResponse;
import org.sky.model.UserEntity;
import org.sky.model.UserRole;
import org.sky.service.cache.CacheService;

@ApplicationScoped
public class DatabaseLoginStrategy implements LoginStrategy {

    @Inject
    UserLoginService userLoginService;
    
    @Inject
    UserValidations userValidationService;
    
    @Inject
    JwtTokenService tokenService;
    
    @Inject
    LoginResponseBuilder loginResponseBuilder;
    
    @Inject
    CacheService cacheService;

    @Override
    public Uni<ApiResponse<LoginResponse>> execute(LoginRequest request) {
        return userLoginService.getUserForLogin(request)
                .chain(user -> userValidationService.validateUserCredentials(user, request))
                .chain(user -> {
                    if (user.role == UserRole.SELLER) {
                        return userLoginService.getSellerForUser(user)
                                .chain(seller -> Uni.combine().all()
                                        .unis(
                                            userLoginService.updateUserLoginInfo(user, request),
                                            tokenService.generateTokensForSeller(user, seller.id)
                                        )
                                        .asTuple()
                                        .chain(tuple -> {
                                            UserEntity updatedUser = tuple.getItem1();
                                            JwtTokenService.TokenData tokenData = tuple.getItem2();

                                            return cacheService.cacheUser(request.email(), request.role().toString(), updatedUser)
                                                    .chain(v -> loginResponseBuilder.buildLoginResponse(tokenData));
                                        }));
                    } else {
                        return Uni.combine().all()
                                .unis(
                                    userLoginService.updateUserLoginInfo(user, request),
                                    tokenService.generateTokens(user)
                                )
                                .asTuple()
                                .chain(tuple -> {
                                    UserEntity updatedUser = tuple.getItem1();
                                    JwtTokenService.TokenData tokenData = tuple.getItem2();

                                    return cacheService.cacheUser(request.email(), request.role().toString(), updatedUser)
                                            .chain(v -> loginResponseBuilder.buildLoginResponse(tokenData));
                                });
                    }
                });
    }
}
