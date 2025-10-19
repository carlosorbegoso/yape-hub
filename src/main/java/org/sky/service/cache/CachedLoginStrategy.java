package org.sky.service.cache;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.response.ApiResponse;
import org.sky.dto.request.auth.LoginRequest;
import org.sky.dto.response.auth.LoginResponse;
import org.sky.model.UserEntityEntity;
import org.sky.service.auth.JwtTokenService;
import org.sky.service.auth.LoginResponseBuilder;
import org.sky.service.auth.LoginStrategy;
import org.sky.service.auth.UserValidations;

@ApplicationScoped
public class CachedLoginStrategy implements LoginStrategy {

    @Inject
    UserValidations userValidationService;
    
    @Inject
    JwtTokenService tokenService;
    
    @Inject
    LoginResponseBuilder loginResponseBuilder;

    @Override
    public Uni<ApiResponse<LoginResponse>> execute(LoginRequest request) {
        return userValidationService.validateUserCredentials(null, request)
                .chain(user -> tokenService.generateTokens(user))
                .chain(tokenData -> loginResponseBuilder.buildLoginResponseFromCachedUser(tokenData));
    }

    public Uni<ApiResponse<LoginResponse>> executeWithCachedUser(UserEntityEntity cachedUser, LoginRequest request) {
        return userValidationService.validateUserCredentials(cachedUser, request)
                .chain(user -> tokenService.generateTokens(user))
                .chain(tokenData -> loginResponseBuilder.buildLoginResponseFromCachedUser(tokenData));
    }
}
