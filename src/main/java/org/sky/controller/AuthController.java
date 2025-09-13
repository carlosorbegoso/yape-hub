package org.sky.controller;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.sky.dto.ApiResponse;
import org.sky.dto.auth.AdminRegisterRequest;
import org.sky.dto.auth.LoginRequest;
import org.sky.service.AuthService;
import org.sky.util.JwtExtractor;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication", description = "Authentication and user management endpoints")
public class AuthController {

  private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    @Inject
    AuthService authService;
    
    @Inject
    JwtExtractor jwtExtractor;
    
    @POST
    @Path("/admin/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @PermitAll
    @Operation(summary = "Register new admin", description = "Register a new business administrator")
    public Uni<Response> registerAdmin(@Valid AdminRegisterRequest request) {
        log.info("Register new administrator");
        return authService.registerAdmin(request)
                .map(response -> Response.status(201).entity(response).build());
    }
    
    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @PermitAll
    @Operation(summary = "User login", description = "Authenticate user and return access token")
    public Uni<Response> login(@Valid LoginRequest request) {
        return authService.login(request)
                .map(response -> Response.ok(response).build());
    }
    
    @POST
    @Path("/refresh")
    @PermitAll
    @Operation(summary = "Refresh token", description = "Generate new access token using refresh token")
    public Uni<Response> refreshToken(@HeaderParam("X-Auth-Token") String token) {
        return authService.refreshToken(token)
                .map(response -> Response.ok(response).build());
    }
    
    @POST
    @Path("/logout")
    @PermitAll
    @Operation(summary = "User logout", description = "Logout user and invalidate session")
    public Uni<Response> logout(@HeaderParam("X-Auth-Token") String token) {
        try {
            Long userId = jwtExtractor.extractUserIdFromToken(token);
            return authService.logout(userId)
                    .map(response -> Response.ok(response).build());
        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage());
            org.sky.dto.ErrorResponse errorResponse = new org.sky.dto.ErrorResponse(
                "Invalid token: " + e.getMessage(),
                "INVALID_TOKEN",
                java.util.Map.of("error", e.getMessage()),
                java.time.Instant.now()
            );
            return Uni.createFrom().item(Response.status(400)
                    .entity(errorResponse)
                    .build());
        }
    }
    
    @POST
    @Path("/forgot-password")
    @Operation(summary = "Forgot password", description = "Send password reset email")
    public Response forgotPassword(@QueryParam("email") String email) {
        // TODO: Implement forgot password functionality
        ApiResponse<String> response = ApiResponse.success("Password reset email sent");
        return Response.ok(response).build();
    }
    
    @POST
    @Path("/seller/login-by-phone")
    @PermitAll
    @Operation(summary = "Seller login by phone", description = "Login for sellers using only their phone number (no password required)")
    public Uni<Response> sellerLoginByPhone(@QueryParam("phone") String phone) {
        return authService.loginByPhone(phone)
                .map(response -> {
                    if (response.isSuccess()) {
                        return Response.ok(response).build();
                    } else {
                        return Response.status(400).entity(response).build();
                    }
                });
    }
    
    @POST
    @Path("/test-token")
    @PermitAll
    @Operation(summary = "Test token extraction", description = "Test JWT token extraction")
    public Response testToken(@HeaderParam("X-Auth-Token") String token) {
        try {
            // Remove "Bearer " prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            // Parse our simple JWT manually
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                org.sky.dto.ErrorResponse errorResponse = new org.sky.dto.ErrorResponse(
                    "Invalid JWT format - expected 3 parts, got " + parts.length,
                    "INVALID_JWT_FORMAT",
                    java.util.Map.of("expectedParts", 3, "actualParts", parts.length),
                    java.time.Instant.now()
                );
                return Response.status(400)
                        .entity(errorResponse)
                        .build();
            }
            
            // Decode the payload (second part)
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            
            // Extract subject using regex (works with both JSON and Map formats)
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("sub=?(\\d+)");
            java.util.regex.Matcher matcher = pattern.matcher(payload);
            
            if (matcher.find()) {
                String subject = matcher.group(1);
                Long userId = Long.parseLong(subject);
                ApiResponse<Object> response = ApiResponse.success("Token extracted successfully", 
                    java.util.Map.of("userId", userId, "payload", payload));
                return Response.ok(response).build();
            } else {
                org.sky.dto.ErrorResponse errorResponse = new org.sky.dto.ErrorResponse(
                    "Subject not found in token payload: " + payload,
                    "SUBJECT_NOT_FOUND",
                    java.util.Map.of("payload", payload),
                    java.time.Instant.now()
                );
                return Response.status(400)
                        .entity(errorResponse)
                        .build();
            }
            
        } catch (Exception e) {
            log.error("Error extracting token: {}", e.getMessage());
            org.sky.dto.ErrorResponse errorResponse = new org.sky.dto.ErrorResponse(
                "Invalid token: " + e.getMessage(),
                "TOKEN_EXTRACTION_ERROR",
                java.util.Map.of("error", e.getMessage()),
                java.time.Instant.now()
            );
            return Response.status(400)
                    .entity(errorResponse)
                    .build();
        }
    }
    
    @POST
    @Path("/change-password")
    @Operation(summary = "Change password", description = "Change user password")
    public Response changePassword(@QueryParam("userId") Long userId, 
                                  @QueryParam("currentPassword") String currentPassword,
                                  @QueryParam("newPassword") String newPassword) {
        // TODO: Implement change password functionality
        ApiResponse<String> response = ApiResponse.success("Password changed successfully");
        return Response.ok(response).build();
    }
    
}
