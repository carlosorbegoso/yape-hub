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

import org.sky.dto.request.admin.AdminRegisterRequest;
import org.sky.dto.request.auth.LoginRequest;
import org.sky.dto.response.ApiResponse;
import org.sky.dto.response.ErrorResponse;
import org.sky.service.auth.AuthService;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
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
    @Operation(
        summary = "Registrar nuevo administrador", 
        description = "Registra un nuevo administrador de negocio en el sistema. El administrador podrá gestionar vendedores, sucursales y pagos."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "201", 
            description = "Administrador registrado exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Registro exitoso",
                    summary = "Respuesta de registro exitoso",
                    value = """
                        {
                          "success": true,
                          "message": "Administrador registrado exitosamente",
                          "data": {
                            "adminId": 1,
                            "businessName": "Mi Negocio",
                            "email": "admin@negocio.com",
                            "contactName": "Juan Pérez",
                            "phone": "+51987654321",
                            "createdAt": "2024-01-15T10:30:00Z"
                          },
                          "timestamp": "2024-01-15T10:30:00Z"
                        }
                        """
                )
            )
        ),
        @APIResponse(
            responseCode = "400", 
            description = "Datos de entrada inválidos",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Error de validación",
                    summary = "Error cuando faltan campos requeridos",
                    value = """
                        {
                          "message": "Email ya registrado",
                          "errorCode": "EMAIL_ALREADY_EXISTS",
                          "details": {
                            "field": "email",
                            "value": "admin@negocio.com",
                            "reason": "El email ya está en uso"
                          },
                          "timestamp": "2024-01-15T10:30:00Z"
                        }
                        """
                )
            )
        )
    })
    public Uni<Response> registerAdmin(@Valid AdminRegisterRequest request) {
      return authService.registerAdmin(request)
          .map(response -> Response.status(Response.Status.CREATED).entity(response).build());
    }
    
    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @PermitAll
    @Operation(
        summary = "Iniciar sesión", 
        description = "Autentica un usuario (administrador o vendedor) y retorna un token JWT de acceso. El token debe incluirse en el header Authorization para endpoints protegidos."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200", 
            description = "Login exitoso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Login exitoso",
                    summary = "Respuesta de login exitoso",
                    value = """
                        {
                          "success": true,
                          "message": "Login exitoso",
                          "data": {
                            "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                            "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                            "userId": 1,
                            "userRole": "admin",
                            "businessName": "Mi Negocio",
                            "expiresIn": 3600
                          },
                          "timestamp": "2024-01-15T10:30:00Z"
                        }
                        """
                )
            )
        ),
        @APIResponse(
            responseCode = "401", 
            description = "Credenciales inválidas",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Credenciales inválidas",
                    summary = "Error de autenticación",
                    value = """
                        {
                          "message": "Credenciales inválidas",
                          "errorCode": "INVALID_CREDENTIALS",
                          "details": {
                            "field": "email",
                            "reason": "Usuario no encontrado o contraseña incorrecta"
                          },
                          "timestamp": "2024-01-15T10:30:00Z"
                        }
                        """
                )
            )
        )
    })
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
    public Uni<Response> forgotPassword(@QueryParam("email") String email) {
        if (email == null || email.trim().isEmpty()) {
            return Uni.createFrom().item(Response.status(400)
                .entity(ApiResponse.error("Email is required")).build());
        }
        
        return authService.forgotPassword(email.trim())
            .map(result -> Response.ok(ApiResponse.success("Password reset email sent", result)).build())
            .onFailure().recoverWithItem(throwable -> Response.status(400)
                .entity(ApiResponse.error("Failed to send password reset email: " + throwable.getMessage())).build());
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
