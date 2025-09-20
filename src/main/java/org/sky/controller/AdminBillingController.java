package org.sky.controller;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.sky.dto.ApiResponse;
import org.sky.dto.billing.PaymentUploadResponse;
import org.sky.repository.ManualPaymentRepository;
import org.sky.repository.PaymentCodeRepository;
import org.sky.service.SecurityService;

@Path("/api/admin/billing")
@Tag(name = "Admin Billing Management", description = "Gestión interna de facturación para administradores")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminBillingController {

    @Inject
    ManualPaymentRepository manualPaymentRepository;

    @Inject
    PaymentCodeRepository paymentCodeRepository;

    @Inject
    SecurityService securityService;

    @GET
    @Operation(summary = "Get admin billing information", description = "Obtiene información de administración de facturación según el tipo")
    public Uni<Response> getAdminBillingInfo(@QueryParam("type") @DefaultValue("dashboard") String type,
                                           @QueryParam("adminId") Long adminId,
                                           @QueryParam("status") @DefaultValue("all") String status,
                                           @QueryParam("include") @DefaultValue("details") String include,
                                           @HeaderParam("Authorization") String authorization) {
        Log.info("🔧 AdminBillingController.getAdminBillingInfo() - Type: " + type + ", AdminId: " + adminId);
        
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> {
                    return switch (type.toLowerCase()) {
                        case "payments" -> getPaymentsByStatus(adminId, status, include);
                        case "codes" -> getPaymentCodesUnified(adminId, include);
                        case "dashboard" -> getAdminDashboardUnified(adminId, include);
                        case "stats" -> getAdminStats(adminId, include);
                        default -> Uni.createFrom().failure(new IllegalArgumentException("Tipo no válido: " + type));
                    };
                })
                .map(response -> Response.ok(response).build())
                .onFailure().recoverWithItem(throwable -> {
                    Log.warn("❌ Error obteniendo información de admin billing: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }

    private Uni<ApiResponse<Object>> getPaymentsByStatus(Long adminId, String status, String include) {
        Log.info("💳 AdminBillingController.getPaymentsByStatus() - Status: " + status);
        
        return switch (status.toLowerCase()) {
            case "pending" -> manualPaymentRepository.findPendingPayments()
                    .map(payments -> ApiResponse.success("Pagos pendientes obtenidos exitosamente", payments));
            case "approved" -> manualPaymentRepository.findByStatus("approved")
                    .map(payments -> ApiResponse.success("Pagos aprobados obtenidos exitosamente", payments));
            case "rejected" -> manualPaymentRepository.findByStatus("rejected")
                    .map(payments -> ApiResponse.success("Pagos rechazados obtenidos exitosamente", payments));
            case "all" -> manualPaymentRepository.findByAdminId(adminId)
                    .map(payments -> ApiResponse.success("Todos los pagos obtenidos exitosamente", payments));
            default -> Uni.createFrom().item(ApiResponse.error("Estado no válido: " + status));
        };
    }

    private Uni<ApiResponse<Object>> getPaymentCodesUnified(Long adminId, String include) {
        Log.info("🔑 AdminBillingController.getPaymentCodesUnified()");
        
        return paymentCodeRepository.findByStatus("pending")
                .map(codes -> ApiResponse.success("Códigos de pago obtenidos exitosamente", codes));
    }

    private Uni<Response> getAdminDashboardUnified(Long adminId, String include) {
        Log.info("📊 AdminBillingController.getAdminDashboardUnified()");
        
        return Uni.combine().all()
                .unis(
                    manualPaymentRepository.countPendingPayments(),
                    manualPaymentRepository.countApprovedPayments(),
                    manualPaymentRepository.countRejectedPayments(),
                    paymentCodeRepository.countPendingCodes()
                )
                .asTuple()
                .map(tuple -> {
                    Long pendingCount = tuple.getItem1();
                    Long approvedCount = tuple.getItem2();
                    Long rejectedCount = tuple.getItem3();
                    Long pendingCodesCount = tuple.getItem4();
                    
                    return ApiResponse.success("Dashboard obtenido exitosamente", 
                        new AdminDashboardStats(pendingCount, approvedCount, rejectedCount, pendingCodesCount));
                })
                .map(apiResponse -> Response.ok(apiResponse).build())
                .onFailure().recoverWithItem(throwable -> {
                    Log.error("❌ Error obteniendo dashboard: " + throwable.getMessage());
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(ApiResponse.error("Error obteniendo dashboard"))
                            .build();
                });
    }

    private Uni<ApiResponse<Object>> getAdminStats(Long adminId, String include) {
        Log.info("📈 AdminBillingController.getAdminStats()");
        
        // TODO: Implementar estadísticas avanzadas
        return Uni.createFrom().item(ApiResponse.success("Estadísticas obtenidas exitosamente", "Stats disponibles"));
    }

    @GET
    @Path("/payments/approved")
    @Operation(summary = "Get approved payments", description = "Obtiene pagos aprobados")
    public Uni<Response> getApprovedPayments(@QueryParam("adminId") Long adminId,
                                           @HeaderParam("Authorization") String authorization) {
        Log.info("✅ AdminBillingController.getApprovedPayments() - AdminId: " + adminId);
        
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> {
                    Log.info("✅ Autorización exitosa para adminId: " + adminId);
                    return manualPaymentRepository.findByStatus("approved");
                })
                .map(approvedPayments -> {
                    Log.info("✅ Pagos aprobados obtenidos: " + approvedPayments.size());
                    return Response.ok(ApiResponse.success("Pagos aprobados obtenidos exitosamente", approvedPayments)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    Log.warn("❌ Error obteniendo pagos aprobados: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }

    @GET
    @Path("/payments/rejected")
    @Operation(summary = "Get rejected payments", description = "Obtiene pagos rechazados")
    public Uni<Response> getRejectedPayments(@QueryParam("adminId") Long adminId,
                                           @HeaderParam("Authorization") String authorization) {
        Log.info("❌ AdminBillingController.getRejectedPayments() - AdminId: " + adminId);
        
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> {
                    Log.info("✅ Autorización exitosa para adminId: " + adminId);
                    return manualPaymentRepository.findByStatus("rejected");
                })
                .map(rejectedPayments -> {
                    Log.info("✅ Pagos rechazados obtenidos: " + rejectedPayments.size());
                    return Response.ok(ApiResponse.success("Pagos rechazados obtenidos exitosamente", rejectedPayments)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    Log.warn("❌ Error obteniendo pagos rechazados: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }

    @PUT
    @Path("/payments/{paymentId}/approve")
    @Operation(summary = "Approve payment", description = "Aprueba un pago manualmente")
    public Uni<Response> approvePayment(@PathParam("paymentId") Long paymentId,
                                      @QueryParam("adminId") Long adminId,
                                      @QueryParam("reviewNotes") String reviewNotes,
                                      @HeaderParam("Authorization") String authorization) {
        Log.info("✅ AdminBillingController.approvePayment() - PaymentId: " + paymentId + ", AdminId: " + adminId);
        
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> {
                    Log.info("✅ Autorización exitosa para adminId: " + adminId);
                    return manualPaymentRepository.findById(paymentId)
                            .chain(payment -> {
                                if (payment == null || !payment.isPending()) {
                                    return Uni.createFrom().failure(new RuntimeException("Pago no encontrado o ya procesado"));
                                }
                                
                                // Aprobar el pago
                                payment.approve(adminId, reviewNotes);
                                
                                return manualPaymentRepository.persist(payment)
                                        .map(savedPayment -> {
                                            Log.info("✅ Pago aprobado exitosamente: " + paymentId);
                                            return new PaymentUploadResponse(
                                                paymentId,
                                                "Pago aprobado exitosamente",
                                                "approved"
                                            );
                                        });
                            });
                })
                .map(response -> {
                    Log.info("✅ Pago aprobado exitosamente");
                    return Response.ok(ApiResponse.success("Pago aprobado exitosamente", response)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    Log.warn("❌ Error aprobando pago: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }

    @PUT
    @Path("/payments/{paymentId}/reject")
    @Operation(summary = "Reject payment", description = "Rechaza un pago manualmente")
    public Uni<Response> rejectPayment(@PathParam("paymentId") Long paymentId,
                                     @QueryParam("adminId") Long adminId,
                                     @QueryParam("reviewNotes") String reviewNotes,
                                     @HeaderParam("Authorization") String authorization) {
        Log.info("❌ AdminBillingController.rejectPayment() - PaymentId: " + paymentId + ", AdminId: " + adminId);
        
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> {
                    Log.info("✅ Autorización exitosa para adminId: " + adminId);
                    return manualPaymentRepository.findById(paymentId)
                            .chain(payment -> {
                                if (payment == null || !payment.isPending()) {
                                    return Uni.createFrom().failure(new RuntimeException("Pago no encontrado o ya procesado"));
                                }
                                
                                // Rechazar el pago
                                payment.reject(adminId, reviewNotes);
                                
                                return manualPaymentRepository.persist(payment)
                                        .map(savedPayment -> {
                                            Log.info("❌ Pago rechazado exitosamente: " + paymentId);
                                            return new PaymentUploadResponse(
                                                paymentId,
                                                "Pago rechazado: " + reviewNotes,
                                                "rejected"
                                            );
                                        });
                            });
                })
                .map(response -> {
                    Log.info("✅ Pago rechazado exitosamente");
                    return Response.ok(ApiResponse.success("Pago rechazado exitosamente", response)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    Log.warn("❌ Error rechazando pago: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }

    @GET
    @Path("/payments/{paymentId}/image")
    @Operation(summary = "Get payment image", description = "Obtiene la imagen del comprobante de pago")
    public Uni<Response> getPaymentImage(@PathParam("paymentId") Long paymentId,
                                       @QueryParam("adminId") Long adminId,
                                       @HeaderParam("Authorization") String authorization) {
        Log.info("📸 AdminBillingController.getPaymentImage() - PaymentId: " + paymentId + ", AdminId: " + adminId);
        
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> {
                    Log.info("✅ Autorización exitosa para adminId: " + adminId);
                    return manualPaymentRepository.findById(paymentId);
                })
                .map(payment -> {
                    if (payment == null) {
                        Log.warn("❌ Pago no encontrado: " + paymentId);
                        return Response.status(404)
                                .entity(ApiResponse.error("Pago no encontrado")).build();
                    }
                    
                    Log.info("✅ Imagen de pago obtenida exitosamente");
                    return Response.ok(ApiResponse.success("Imagen de pago obtenida exitosamente", payment.imageBase64)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    Log.warn("❌ Error obteniendo imagen de pago: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }

    @GET
    @Path("/payments/codes")
    @Operation(summary = "Get payment codes", description = "Obtiene todos los códigos de pago generados")
    public Uni<Response> getPaymentCodes(@QueryParam("adminId") Long adminId,
                                       @HeaderParam("Authorization") String authorization) {
        Log.info("🔑 AdminBillingController.getPaymentCodes() - AdminId: " + adminId);
        
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> {
                    Log.info("✅ Autorización exitosa para adminId: " + adminId);
                    return paymentCodeRepository.findByStatus("pending");
                })
                .map(paymentCodes -> {
                    Log.info("✅ Códigos de pago obtenidos: " + paymentCodes.size());
                    return Response.ok(ApiResponse.success("Códigos de pago obtenidos exitosamente", paymentCodes)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    Log.warn("❌ Error obteniendo códigos de pago: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }

    @GET
    @Path("/dashboard")
    @Operation(summary = "Get admin billing dashboard", description = "Obtiene el dashboard de administración de pagos")
    public Uni<Response> getAdminDashboard(@QueryParam("adminId") Long adminId,
                                          @HeaderParam("Authorization") String authorization) {
        Log.info("📊 AdminBillingController.getAdminDashboard() - AdminId: " + adminId);
        
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> {
                    Log.info("✅ Autorización exitosa para adminId: " + adminId);
                    
                    // Obtener estadísticas del dashboard
                    return Uni.combine().all()
                            .unis(
                                manualPaymentRepository.countPendingPayments(),
                                manualPaymentRepository.countApprovedPayments(),
                                manualPaymentRepository.countRejectedPayments(),
                                paymentCodeRepository.countPendingCodes()
                            )
                            .asTuple()
                            .map(tuple -> {
                                Long pendingCount = tuple.getItem1();
                                Long approvedCount = tuple.getItem2();
                                Long rejectedCount = tuple.getItem3();
                                Long pendingCodesCount = tuple.getItem4();
                                
                                return Response.ok(ApiResponse.success("Dashboard obtenido exitosamente", 
                                    new AdminDashboardStats(pendingCount, approvedCount, rejectedCount, pendingCodesCount))).build();
                            });
                })
                .onFailure().recoverWithItem(throwable -> {
                    Log.warn("❌ Error obteniendo dashboard: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }


    // Clase interna para estadísticas del dashboard
    public record AdminDashboardStats(
        Long pendingPayments,
        Long approvedPayments,
        Long rejectedPayments,
        Long pendingCodes
    ) {}
}
