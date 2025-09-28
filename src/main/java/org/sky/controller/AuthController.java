package org.sky.controller;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.sky.dto.ApiResponse;
import org.sky.dto.ErrorResponse;
import org.sky.dto.auth.AdminRegisterRequest;
import org.sky.dto.auth.LoginRequest;
import org.sky.service.auth.AuthService;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.sky.util.jwt.JwtExtractor;

import java.time.Instant;


@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication", description = "Authentication and user management endpoints")
public class AuthController {

  @Inject
  AuthService authService;
    
  @Inject
  JwtExtractor  jwtExtractor;

  @Inject
  JsonWebToken jwt;
    
    @POST
    @Path("/admin/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @PermitAll
    @Operation(summary = "Register new admin", description = "Register a new business administrator")
    public Uni<Response> registerAdmin(@Valid AdminRegisterRequest request) {
      return authService.registerAdmin(request)
          .map(response -> Response.status(Response.Status.CREATED).entity(response).build());
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
  public Uni<Response> logout() {
    return jwtExtractor.extractUserId(jwt)
        .onItem().ifNull().failWith(() -> new RuntimeException("UserId not found in token"))
        .chain(userId ->
            authService.logout(userId)
                .map(result -> Response.ok(result).build())
        )
        .onFailure().recoverWithItem(throwable ->
            Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(
                    "Invalid token",
                    "INVALID_TOKEN",
                    java.util.Map.of("error", "UserId not found in token"),
                    Instant.now()
                ))
                .build()
        );
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
    @Operation(summary = "Seller login by phone with affiliation code", description = "Login for sellers using phone number and affiliation code for enhanced security")
    public Uni<Response> sellerLoginByPhone(
            @QueryParam("phone") @NotBlank(message = "El número de teléfono es requerido") String phone,
            @QueryParam("affiliationCode") @NotBlank(message = "El código de afiliación es requerido") String affiliationCode) {
        return authService.loginByPhoneWithAffiliation(phone, affiliationCode)
                .map(response -> {
                    if (response.isSuccess()) {
                        return Response.ok(response).build();
                    } else {
                      return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
                    }
                });
    }
    


}
