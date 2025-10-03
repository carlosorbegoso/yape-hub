package org.sky.controller;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.sky.dto.request.billing.PaymentRequest;
import org.sky.dto.response.ApiResponse;
import org.sky.service.subscription.PaymentCodeService;
import org.sky.service.subscription.PaymentUploadService;
import org.sky.service.security.SecurityService;
import org.sky.service.billing.BillingDashboardService;
import org.sky.service.billing.BillingPlansService;
import org.sky.service.billing.BillingOperationsService;
import org.sky.service.billing.BillingInfoService;

@Path("/api/billing")
@Tag(name = "Billing Management", description = "Gestión de facturación y tokens para usuarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BillingController {

    @Inject
    PaymentCodeService paymentCodeService;
    
    @Inject
    PaymentUploadService paymentUploadService;
    
    @Inject
    SecurityService securityService;
    
    @Inject
    BillingDashboardService billingDashboardService;
    
    @Inject
    BillingPlansService billingPlansService;
    
    @Inject
    BillingOperationsService billingOperationsService;
    
    @Inject
    BillingInfoService billingInfoService;

    @GET
    @Operation(summary = "Get billing information", description = "Obtiene información de facturación, tokens, suscripciones o dashboard según el tipo")
    @WithTransaction
    public Uni<Response> getBillingInfo(@QueryParam("type") @DefaultValue("dashboard") String type,
                                       @QueryParam("adminId") Long adminId,
                                       @QueryParam("period") @DefaultValue("current") String period,
                                       @QueryParam("include") @DefaultValue("details") String include,
                                       @QueryParam("startDate") String startDateStr,
                                       @QueryParam("endDate") String endDateStr,
                                       @HeaderParam("Authorization") String authorization) {
        
        if ("plans".equals(type.toLowerCase()) && adminId == null) {
            return billingPlansService.getAvailablePlans(include)
                    .map(response -> Response.ok(response).build());
        }
        
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> {
                    return switch (type.toLowerCase()) {
                        case "subscription" -> billingInfoService.getSubscriptionStatus(adminId, period, include);
                        case "payments" -> billingInfoService.getPaymentHistory(adminId, period, include);
                        case "dashboard" -> billingDashboardService.getDashboard(adminId, period, include);
                        case "plans" -> billingPlansService.getAvailablePlans(include);
                        default -> Uni.createFrom().failure(new IllegalArgumentException("Tipo no válido: " + type));
                    };
                })
                .map(response -> Response.ok(response).build())
                .onFailure().recoverWithItem(throwable -> securityService.handleSecurityException(throwable));
    }

    @POST
    @Path("/operations")
    @Operation(summary = "Execute billing operations", description = "Ejecuta operaciones de facturación como generar códigos, subir imágenes, etc.")
    @WithTransaction
    public Uni<Response> executeOperation(@QueryParam("adminId") Long adminId,
                                         @QueryParam("action") String action,
                                         @QueryParam("validate") @DefaultValue("true") Boolean validate,
                                         @HeaderParam("Authorization") String authorization,
                                         PaymentRequest request) {
        
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> switch (action.toLowerCase()) {
                    case "generate-code" -> billingOperationsService.executeGenerateCode(adminId, request, validate);
                    case "upload" -> billingOperationsService.executeUpload(adminId, request, validate);
                    case "subscribe" -> billingOperationsService.executeSubscribe(adminId, request, validate);
                    case "upgrade" -> billingOperationsService.executeUpgrade(adminId, request, validate);
                    case "cancel" -> billingOperationsService.executeCancel(adminId, request, validate);
                    case "check" -> billingOperationsService.executeCheck(adminId, request);
                    case "simulate" -> billingOperationsService.executeSimulate(adminId, request);
                    default -> Uni.createFrom().failure(new IllegalArgumentException("Acción no válida: " + action));
                })
                .map(response -> Response.ok(response).build())
                .onFailure().recoverWithItem(throwable -> securityService.handleSecurityException(throwable));
    }

    @POST
    @Path("/payments/upload")
    @Operation(summary = "Upload payment image", description = "Sube la imagen del comprobante de pago en formato base64")
    @WithSession
    public Uni<Response> uploadPaymentImage(@QueryParam("adminId") Long adminId,
                                          @QueryParam("paymentCode") String paymentCode,
                                          PaymentRequest request,
                                          @HeaderParam("Authorization") String authorization) {
        
        if (paymentCode == null || request.imageBase64() == null) {
            return Uni.createFrom().item(Response.status(400)
                    .entity(ApiResponse.error("paymentCode e imageBase64 son requeridos")).build());
        }
        
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> paymentUploadService.uploadPaymentImage(adminId, paymentCode, request.imageBase64()))
                .map(uploadResponse -> Response.ok(ApiResponse.success("Imagen de pago subida exitosamente", uploadResponse)).build())
                .onFailure().recoverWithItem(throwable -> securityService.handleSecurityException(throwable));
    }

    @GET
    @Path("/payments/status/{paymentCode}")
    @Operation(summary = "Get payment status", description = "Obtiene el estado de un pago por código")
    @WithSession
    public Uni<Response> getPaymentStatus(@PathParam("paymentCode") String paymentCode,
                                        @HeaderParam("Authorization") String authorization) {
        
        return paymentCodeService.getPaymentStatus(paymentCode)
                .map(statusResponse -> Response.ok(ApiResponse.success("Estado de pago obtenido exitosamente", statusResponse)).build())
                .onFailure().recoverWithItem(throwable -> Response.status(400)
                        .entity(ApiResponse.error("Error obteniendo estado de pago: " + throwable.getMessage())).build());
    }

    @GET
    @Path("/plans")
    @Operation(summary = "Get available plans", description = "Obtiene los planes de suscripción disponibles")
    public Uni<Response> getAvailablePlans() {
        return billingPlansService.getAvailablePlans("details")
                .map(response -> Response.ok(response).build());
    }

    @GET
    @Path("/dashboard")
    @Operation(summary = "Get billing dashboard", description = "Obtiene el dashboard de facturación del administrador")
    public Uni<Response> getBillingDashboard(@QueryParam("adminId") Long adminId,
                                           @HeaderParam("Authorization") String authorization) {
        
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> billingDashboardService.getDashboard(adminId, "current", "details"))
                .map(response -> Response.ok(response).build())
                .onFailure().recoverWithItem(throwable -> securityService.handleSecurityException(throwable));
    }

    @POST
    @Path("/load-data")
    @Operation(summary = "Load subscription plans and token packages", description = "Carga los planes de suscripción y paquetes de tokens en la base de datos")
    public Uni<Response> loadData() {
        return billingPlansService.loadData()
                .map(response -> Response.ok(response).build())
                .onFailure().recoverWithItem(throwable -> Response.status(500)
                        .entity(ApiResponse.error("Error creando planes: " + throwable.getMessage()))
                        .build());
    }
}