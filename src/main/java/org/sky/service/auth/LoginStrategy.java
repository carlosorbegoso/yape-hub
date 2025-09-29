package org.sky.service.auth;

import io.smallrye.mutiny.Uni;
import org.sky.dto.ApiResponse;
import org.sky.dto.auth.LoginRequest;
import org.sky.dto.auth.LoginResponse;

public interface LoginStrategy {
    Uni<ApiResponse<LoginResponse>> execute(LoginRequest request);
}
