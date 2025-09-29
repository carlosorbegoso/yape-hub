package org.sky.service.auth;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.ApiResponse;
import org.sky.dto.auth.LoginRequest;
import org.sky.dto.auth.LoginResponse;
import org.sky.model.User;
import org.sky.service.CacheService;

@ApplicationScoped
public class DatabaseLoginStrategy implements LoginStrategy {

    @Inject
    UserLoginService userLoginService;
    
    @Inject
    UserValidations userValidationService;
    
    @Inject
    TokenService tokenService;
    
    @Inject
    LoginResponseBuilder loginResponseBuilder;
    
    @Inject
    CacheService cacheService;

    @Override
    public Uni<ApiResponse<LoginResponse>> execute(LoginRequest request) {
        return userLoginService.getUserForLogin(request)
                .chain(user -> userValidationService.validateUserCredentials(user, request))
                .chain(user -> {
                    if (user.role == User.UserRole.SELLER) {
                        return userLoginService.getSellerForUser(user)
                                .chain(seller -> Uni.combine().all()
                                        .unis(
                                            userLoginService.updateUserLoginInfo(user, request),
                                            tokenService.generateTokensForSeller(user, seller.id)
                                        )
                                        .asTuple()
                                        .chain(tuple -> {
                                            User updatedUser = tuple.getItem1();
                                            TokenService.TokenData tokenData = tuple.getItem2();

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
                                    User updatedUser = tuple.getItem1();
                                    TokenService.TokenData tokenData = tuple.getItem2();

                                    return cacheService.cacheUser(request.email(), request.role().toString(), updatedUser)
                                            .chain(v -> loginResponseBuilder.buildLoginResponse(tokenData));
                                });
                    }
                });
    }
}
