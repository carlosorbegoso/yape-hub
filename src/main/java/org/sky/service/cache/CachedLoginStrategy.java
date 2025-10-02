package org.sky.service.cache;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.ApiResponse;
import org.sky.dto.auth.LoginRequest;
import org.sky.dto.auth.LoginResponse;
import org.sky.model.UserEntity;
import org.sky.model.UserRole;
import org.sky.service.auth.LoginStrategy;

@ApplicationScoped
public class CachedLoginStrategy implements LoginStrategy {

    @Inject
    org.sky.service.auth.UserValidations userValidationService;
    
    @Inject
    org.sky.service.auth.JwtTokenService tokenService;
    
    @Inject
    org.sky.service.auth.LoginResponseBuilder loginResponseBuilder;

    @Override
    public Uni<ApiResponse<LoginResponse>> execute(LoginRequest request) {
        return userValidationService.validateUserCredentials(null, request)
                .chain(user -> tokenService.generateTokens(user))
                .chain(tokenData -> loginResponseBuilder.buildLoginResponseFromCachedUser(tokenData));
    }

    public Uni<ApiResponse<LoginResponse>> executeWithCachedUser(UserEntity cachedUser, LoginRequest request) {
        return userValidationService.validateUserCredentials(cachedUser, request)
                .chain(user -> tokenService.generateTokens(user))
                .chain(tokenData -> loginResponseBuilder.buildLoginResponseFromCachedUser(tokenData));
    }
}
