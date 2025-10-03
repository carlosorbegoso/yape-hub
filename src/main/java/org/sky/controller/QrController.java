package org.sky.controller;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.annotation.security.PermitAll;
import org.sky.dto.qr.*;
import org.sky.dto.ErrorResponse;
import org.sky.dto.seller.AffiliateSellerRequest;
import org.sky.service.QrService;
import org.sky.service.security.SecurityService;
import org.sky.service.SellerService;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;

import java.util.Map;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Affiliation Management", description = "Affiliation code management endpoints for admin and sellers")
public class QrController {
    
    private static final Logger log = Logger.getLogger(QrController.class);
    
    /**
     * Convierte un ApiResponse de error a ErrorResponse
     */
    private ErrorResponse convertToErrorResponse(org.sky.dto.ApiResponse<?> apiResponse) {
        return new ErrorResponse(
            apiResponse.message(),
            "INVALID_FIELD",
            Map.of("reason", apiResponse.message()),
            java.time.Instant.now()
        );
    }
    
    @Inject
    QrService qrService;
    
    @Inject
    SecurityService securityService;
    
    @Inject
    SellerService sellerService;
    
    @POST
    @Path("/generate-affiliation-code-protected")
    @PermitAll
    @Operation(summary = "Generate affiliation code (protected)",
               description = "Generate a new affiliation code for sellers with JWT validation (ADMIN ONLY)")
    @APIResponses(value = {
        @APIResponse(responseCode = "201", description = "Affiliation code generated successfully"),
        @APIResponse(responseCode = "400", description = "Bad request - invalid parameters"),
        @APIResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token"),
        @APIResponse(responseCode = "403", description = "Forbidden - user not authorized for this adminId")
    })
    public Uni<Response> generateAffiliationCodeProtected(
            @Parameter(description = "JWT Bearer token", required = true, in = ParameterIn.HEADER)
            @HeaderParam("Authorization") String authorization,

            @Parameter(description = "Admin ID", required = true, schema = @Schema(type = SchemaType.INTEGER))
            @QueryParam("adminId") Long adminId,

            @Parameter(description = "Expiration hours", schema = @Schema(type = SchemaType.INTEGER))
            @QueryParam("expirationHours") Integer expirationHours,

            @Parameter(description = "Maximum uses", schema = @Schema(type = SchemaType.INTEGER))
            @QueryParam("maxUses") Integer maxUses,

            @Parameter(description = "Branch ID", schema = @Schema(type = SchemaType.INTEGER))
            @QueryParam("branchId") Long branchId,

            @Parameter(description = "Additional notes")
            @QueryParam("notes") String notes) {

        log.info("🚀 QrController.generateAffiliationCodeProtected() - Endpoint llamado");
        log.info("🚀 Parámetros recibidos:");
        log.info("🚀   - adminId: " + adminId);
        log.info("🚀   - expirationHours: " + expirationHours);
        log.info("🚀   - maxUses: " + maxUses);
        log.info("🚀   - branchId: " + branchId);
        log.info("🚀   - notes: " + notes);
        log.info("🚀   - authorization: " + (authorization != null ? authorization.substring(0, Math.min(20, authorization.length())) + "..." : "null"));

        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> {
                    log.info("🚀 Validación exitosa, llamando a QrService.generateAffiliationCode()");
                    return qrService.generateAffiliationCode(adminId, expirationHours, maxUses, branchId, notes);
                })
                .map(response -> {
                    log.info("🚀 Respuesta del servicio recibida - success: " + response.isSuccess());
                    if (response.isSuccess()) {
                        log.info("✅ Retornando respuesta exitosa (201)");
                        return Response.status(201).entity(response).build();
                    } else {
                        log.warn("⚠️ Retornando respuesta de error (400)");
                        return Response.status(400).entity(response).build();
                    }
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.error("❌ Error en el endpoint, manejando excepción");
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    @POST
    @Path("/validate-affiliation-code")
    @PermitAll
    @WithTransaction
    @Operation(summary = "Validate affiliation code", description = "Validate an affiliation code (public endpoint for sellers)")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Affiliation code validated successfully"),
        @APIResponse(responseCode = "400", description = "Bad request - invalid affiliation code")
    })
    public Uni<Response> validateAffiliationCode(ValidateAffiliationCodeRequest request) {
        
        log.info("🚀 QrController.validateAffiliationCode() - Endpoint llamado");
        log.info("🚀 Parámetros recibidos:");
        log.info("🚀   - affiliationCode: " + request.affiliationCode());
        
        return qrService.validateAffiliationCode(request.affiliationCode())
                .map(response -> {
                    log.info("🚀 Respuesta del servicio recibida - success: " + response.isSuccess());
                    if (response.isSuccess()) {
                        log.info("✅ Retornando respuesta exitosa (200)");
                        return Response.ok(response).build();
                    } else {
                        log.warn("⚠️ Retornando respuesta de error (400)");
                        return Response.status(400).entity(response).build();
                    }
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.error("❌ Error en validateAffiliationCode: " + throwable.getMessage());
                    if (throwable instanceof org.sky.exception.ValidationException) {
                        org.sky.exception.ValidationException validationException = (org.sky.exception.ValidationException) throwable;
                        org.sky.dto.ErrorResponse errorResponse = new org.sky.dto.ErrorResponse(
                            validationException.getMessage(),
                            validationException.getErrorCode(),
                            validationException.getDetails(),
                            java.time.Instant.now()
                        );
                        return Response.status(validationException.getStatus()).entity(errorResponse).build();
                    }
                    return Response.status(400).entity(new org.sky.dto.ErrorResponse(
                        "Error interno del servidor",
                        "INTERNAL_ERROR",
                        java.util.Map.of("error", throwable.getMessage()),
                        java.time.Instant.now()
                    )).build();
                });
    }
    
    @POST
    @Path("/seller/register")
    @PermitAll
    @WithTransaction
    @Operation(summary = "Register seller with affiliation code", description = "Register a new seller using ONLY an affiliation code (no direct registration allowed)")
    @APIResponses(value = {
        @APIResponse(responseCode = "201", description = "Seller registered successfully"),
        @APIResponse(responseCode = "400", description = "Bad request - invalid affiliation code or seller data"),
        @APIResponse(responseCode = "404", description = "Affiliation code not found or expired")
    })
    public Uni<Response> registerSeller(@Valid AffiliateSellerRequest request) {
        
        log.info("🚀 QrController.registerSeller() - Registro de vendedor con código de afiliación");
        log.info("🚀 Parámetros recibidos:");
        log.info("🚀   - sellerName: " + request.sellerName());
        log.info("🚀   - phone: " + request.phone());
        log.info("🚀   - affiliationCode: " + request.affiliationCode());
        
        // Primero validamos el código de afiliación
        return qrService.validateAffiliationCode(request.affiliationCode())
                .chain(validationResponse -> {
                    if (!validationResponse.isSuccess()) {
                        log.warn("❌ Código de afiliación inválido: " + validationResponse.message());
                        ErrorResponse errorResponse = new ErrorResponse(
                            "Invalid affiliationCode: " + request.affiliationCode() + " - " + validationResponse.message(),
                            "INVALID_FIELD",
                            Map.of(
                                "field", "affiliationCode",
                                "value", request.affiliationCode(),
                                "reason", validationResponse.message()
                            ),
                            java.time.Instant.now()
                        );
                        return Uni.createFrom().item(Response.status(400).entity(errorResponse).build());
                    }
                    
                                log.info("✅ Código de afiliación válido, procediendo con el registro del vendedor");

                                // Si el código es válido, procedemos con el registro del vendedor
                                // Necesitamos obtener el adminId del código de afiliación
                                return qrService.getAdminIdFromAffiliationCode(request.affiliationCode())
                                        .chain(adminId -> {
                                            log.info("🚀 AdminId obtenido del código: " + adminId);
                                            return sellerService.affiliateSeller(adminId, request);
                                        })
                                        .map(sellerResponse -> {
                                            log.info("🚀 Respuesta del servicio de registro - success: " + sellerResponse.isSuccess());
                                            if (sellerResponse.isSuccess()) {
                                                log.info("✅ Vendedor registrado exitosamente (201)");
                                                return Response.status(201).entity(sellerResponse).build();
                                            } else {
                                                log.warn("⚠️ Error en el registro del vendedor (400)");
                                                ErrorResponse errorResponse = new ErrorResponse(
                                                    sellerResponse.message(),
                                                    "INVALID_FIELD",
                                                    Map.of(
                                                        "field", "sellerRegistration",
                                                        "value", request.toString(),
                                                        "reason", sellerResponse.message()
                                                    ),
                                                    java.time.Instant.now()
                                                );
                                                return Response.status(400).entity(errorResponse).build();
                                            }
                                        });
                });
    }

    @POST
    @Path("/generate-qr-base64")
    @PermitAll
    @Operation(summary = "Generate QR code with Base64", description = "Generates a QR code containing affiliation code encoded in Base64")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "QR generated successfully"),
        @APIResponse(responseCode = "400", description = "Bad request - invalid affiliation code")
    })
    public Uni<Response> generateQrBase64(@Valid ValidateAffiliationCodeRequest request) {
        log.info("🚀 QrController.generateQrBase64() - Endpoint llamado");
        log.info("🚀 Parámetros recibidos:");
        log.info("🚀   - affiliationCode: " + request.affiliationCode());
        
        return qrService.generateQrBase64(request.affiliationCode())
                .map(result -> {
                    log.info("🚀 QR Base64 generado exitosamente");
                    return Response.ok(result).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.error("❌ Error generando QR Base64: " + throwable.getMessage());
                    return Response.status(400).entity(new org.sky.dto.ErrorResponse(
                        "Error generando QR: " + throwable.getMessage(),
                        "QR_GENERATION_ERROR",
                        java.util.Map.of("error", throwable.getMessage()),
                        java.time.Instant.now()
                    )).build();
                });
    }

    @POST
    @Path("/login-with-qr")
    @PermitAll
    @Operation(summary = "Login with QR code", description = "Login seller using QR code containing Base64 encoded affiliation code")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Login successful"),
        @APIResponse(responseCode = "400", description = "Bad request - invalid QR data or phone")
    })
    public Uni<Response> loginWithQr(@Valid QrLoginRequest request) {
        log.info("🚀 QrController.loginWithQr() - Endpoint llamado");
        log.info("🚀 Parámetros recibidos:");
        log.info("🚀   - qrData: " + (request.qrData() != null ? request.qrData().substring(0, Math.min(50, request.qrData().length())) + "..." : "null"));
        log.info("🚀   - phone: " + request.phone());
        
        return qrService.loginWithQr(request.qrData(), request.phone())
                .map(result -> {
                    log.info("🚀 Login con QR exitoso");
                    return Response.ok(result).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.error("❌ Error en login con QR: " + throwable.getMessage());
                    return Response.status(400).entity(new org.sky.dto.ErrorResponse(
                        "Error en login: " + throwable.getMessage(),
                        "QR_LOGIN_ERROR",
                        java.util.Map.of("error", throwable.getMessage()),
                        java.time.Instant.now()
                    )).build();
                });
    }
}
