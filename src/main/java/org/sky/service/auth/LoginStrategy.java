package org.sky.service.auth;

import io.smallrye.mutiny.Uni;

import org.sky.dto.request.auth.LoginRequest;
import org.sky.dto.response.ApiResponse;
import org.sky.dto.response.auth.LoginResponse;

public interface LoginStrategy {
    Uni<ApiResponse<LoginResponse>> execute(LoginRequest request);
}
