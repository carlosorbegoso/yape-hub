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
public class CachedLoginStrategy implements LoginStrategy {

    @Inject
    UserValidations userValidationService;
    
    @Inject
    TokenService tokenService;
    
    @Inject
    LoginResponseBuilder loginResponseBuilder;

    @Override
    public Uni<ApiResponse<LoginResponse>> execute(LoginRequest request) {
        return userValidationService.validateUserCredentials(null, request)
                .chain(user -> tokenService.generateTokens(user))
                .chain(tokenData -> loginResponseBuilder.buildLoginResponseFromCachedUser(tokenData));
    }

    public Uni<ApiResponse<LoginResponse>> executeWithCachedUser(User cachedUser, LoginRequest request) {
        return userValidationService.validateUserCredentials(cachedUser, request)
                .chain(user -> tokenService.generateTokens(user))
                .chain(tokenData -> loginResponseBuilder.buildLoginResponseFromCachedUser(tokenData));
    }
}
