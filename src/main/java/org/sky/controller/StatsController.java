package org.sky.controller;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import org.sky.dto.response.ApiResponse;
import org.sky.service.security.SecurityService;
import org.sky.service.StatsService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Path("/api/stats")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Statistics", description = "Sales statistics and analytics endpoints")
@SecurityRequirement(name = "bearerAuth")
public class StatsController {
    
    @Inject
    StatsService statsService;
    
    @Inject
    SecurityService securityService;
    
    private static final Logger log = Logger.getLogger(StatsController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @GET
    @Path("/admin/summary")
    @Operation(summary = "Get admin basic statistics", 
               description = "Obtiene estadísticas básicas de ventas para un administrador")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Estadísticas obtenidas exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado"),
        @APIResponse(responseCode = "400", description = "Parámetros inválidos")
    })
    public Uni<Response> getAdminSummary(@QueryParam("adminId") Long adminId,
                                         @QueryParam("startDate") String startDateStr,
                                         @QueryParam("endDate") String endDateStr,
                                         @HeaderParam("Authorization") String authorization) {
        log.info("📊 StatsController.getAdminSummary() - AdminId: " + adminId);
        log.info("📊 Desde: " + startDateStr + ", Hasta: " + endDateStr);
        
        // Validar parámetros de fecha
        LocalDate startDate, endDate;
        try {
            startDate = startDateStr != null ? LocalDate.parse(startDateStr, DATE_FORMATTER) : LocalDate.now().minusDays(30);
            endDate = endDateStr != null ? LocalDate.parse(endDateStr, DATE_FORMATTER) : LocalDate.now();
        } catch (DateTimeParseException e) {
            log.warn("❌ Fechas inválidas: " + e.getMessage());
            return Uni.createFrom().item(Response.status(400)
                    .entity(ApiResponse.error("Formato de fecha inválido. Use yyyy-MM-dd")).build());
        }
        
        // Validar autorización de admin
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> {
                    log.info("✅ Autorización exitosa para adminId: " + adminId);
                    return statsService.getAdminStats(adminId, startDate, endDate);
                })
                .map(stats -> {
                    log.info("✅ Estadísticas básicas de admin obtenidas exitosamente");
                    return Response.ok(ApiResponse.success("Estadísticas básicas de admin obtenidas exitosamente", stats)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo estadísticas de admin: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    @GET
    @Path("/seller/summary")
    @Operation(summary = "Get seller basic statistics", 
               description = "Obtiene estadísticas básicas de ventas para un vendedor")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Estadísticas obtenidas exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado"),
        @APIResponse(responseCode = "400", description = "Parámetros inválidos"),
        @APIResponse(responseCode = "404", description = "Vendedor no encontrado")
    })
    public Uni<Response> getSellerSummary(@QueryParam("sellerId") Long sellerId,
                                          @QueryParam("startDate") String startDateStr,
                                          @QueryParam("endDate") String endDateStr,
                                          @HeaderParam("Authorization") String authorization) {
        log.info("📊 StatsController.getSellerSummary() - SellerId: " + sellerId);
        log.info("📊 Desde: " + startDateStr + ", Hasta: " + endDateStr);
        
        // Validar parámetros de fecha
        LocalDate startDate, endDate;
        try {
            startDate = startDateStr != null ? LocalDate.parse(startDateStr, DATE_FORMATTER) : LocalDate.now().minusDays(30);
            endDate = endDateStr != null ? LocalDate.parse(endDateStr, DATE_FORMATTER) : LocalDate.now();
        } catch (DateTimeParseException e) {
            log.warn("❌ Fechas inválidas: " + e.getMessage());
            return Uni.createFrom().item(Response.status(400)
                    .entity(ApiResponse.error("Formato de fecha inválido. Use yyyy-MM-dd")).build());
        }
        
        // Validar autorización del vendedor
        return securityService.validateSellerAuthorization(authorization, sellerId)
                .chain(userId -> {
                    log.info("✅ Autorización exitosa para sellerId: " + sellerId);
                    return statsService.getSellerStats(sellerId, startDate, endDate);
                })
                .map(stats -> {
                    log.info("✅ Estadísticas básicas de vendedor obtenidas exitosamente");
                    return Response.ok(ApiResponse.success("Estadísticas básicas de vendedor obtenidas exitosamente", stats)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo estadísticas de vendedor: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    @GET
    @Path("/admin/dashboard")
    @Operation(summary = "Get admin dashboard summary", 
               description = "Obtiene un resumen rápido de estadísticas para el dashboard del admin con filtros opcionales")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Resumen de dashboard obtenido exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado"),
        @APIResponse(responseCode = "400", description = "Parámetros inválidos")
    })
    public Uni<Response> getAdminDashboard(@QueryParam("adminId") Long adminId,
                                           @QueryParam("startDate") String startDateStr,
                                           @QueryParam("endDate") String endDateStr,
                                           @HeaderParam("Authorization") String authorization) {
        log.info("📊 StatsController.getAdminDashboard() - AdminId: " + adminId);
        log.info("📊 Desde: " + startDateStr + ", Hasta: " + endDateStr);
        
        // Validar parámetros de fecha
        final LocalDate startDate, endDate;
        try {
            if (startDateStr != null && endDateStr != null) {
                startDate = LocalDate.parse(startDateStr, DATE_FORMATTER);
                endDate = LocalDate.parse(endDateStr, DATE_FORMATTER);
            } else {
                // Default: último mes
                endDate = LocalDate.now();
                startDate = endDate.minusDays(30);
            }
        } catch (DateTimeParseException e) {
            log.warn("❌ Fechas inválidas: " + e.getMessage());
            return Uni.createFrom().item(Response.status(400)
                    .entity(ApiResponse.error("Formato de fecha inválido. Use yyyy-MM-dd")).build());
        }
        
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> {
                    log.info("✅ Autorización exitosa para adminId: " + adminId);
                    return statsService.getQuickSummary(adminId, startDate, endDate);
                })
                .map(summary -> {
                    log.info("✅ Resumen de dashboard obtenido exitosamente");
                    return Response.ok(ApiResponse.success("Resumen de dashboard obtenido exitosamente", summary)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo resumen de dashboard: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    
    @GET
    @Path("/admin/analytics")
    @Operation(summary = "Get complete admin analytics", 
               description = "Obtiene analytics completos con ventas diarias, top vendedores, métricas avanzadas y insights administrativos. Parámetros opcionales para análisis específicos.")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Analytics completos obtenidos exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado"),
        @APIResponse(responseCode = "400", description = "Parámetros inválidos")
    })
    public Uni<Response> getAdminAnalytics(@QueryParam("adminId") Long adminId,
                                          @QueryParam("startDate") String startDateStr,
                                          @QueryParam("endDate") String endDateStr,
                                          @QueryParam("include") String include,
                                          @QueryParam("period") String period,
                                          @QueryParam("metric") String metric,
                                          @QueryParam("granularity") String granularity,
                                          @QueryParam("confidence") Double confidence,
                                          @QueryParam("days") Integer days,
                                          @HeaderParam("Authorization") String authorization) {
        log.info("📊 StatsController.getAdminAnalytics() - AdminId: " + adminId);
        log.info("📊 Desde: " + startDateStr + ", Hasta: " + endDateStr);
        log.info("📊 Include: " + include + ", Period: " + period + ", Metric: " + metric);
        log.info("📊 Granularity: " + granularity + ", Confidence: " + confidence + ", Days: " + days);
        
        // Validar parámetros de fecha
        LocalDate startDate, endDate;
        try {
            startDate = startDateStr != null ? LocalDate.parse(startDateStr, DATE_FORMATTER) : LocalDate.now().minusDays(30);
            endDate = endDateStr != null ? LocalDate.parse(endDateStr, DATE_FORMATTER) : LocalDate.now();
        } catch (DateTimeParseException e) {
            log.warn("❌ Fechas inválidas: " + e.getMessage());
            return Uni.createFrom().item(Response.status(400)
                    .entity(ApiResponse.error("Formato de fecha inválido. Use yyyy-MM-dd")).build());
        }
        
        // Validar autorización de admin
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> {
                    log.info("✅ Autorización exitosa para adminId: " + adminId);
                    return statsService.getAnalyticsSummary(adminId, startDate, endDate, include, period, metric, granularity, confidence, days);
                })
                .map(analytics -> {
                    log.info("✅ Analytics completos obtenidos exitosamente");
                    return Response.ok(ApiResponse.success("Analytics completos obtenidos exitosamente", analytics)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo analytics: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    
    @GET
    @Path("/seller/analytics")
    @Operation(summary = "Get seller analytics summary", 
               description = "Obtiene resumen completo de analytics para un vendedor específico. Parámetros opcionales para análisis específicos.")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Analytics de vendedor obtenidos exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado"),
        @APIResponse(responseCode = "400", description = "Parámetros inválidos")
    })
    public Uni<Response> getSellerAnalyticsSummary(@QueryParam("sellerId") Long sellerId,
                                                   @QueryParam("startDate") String startDateStr,
                                                   @QueryParam("endDate") String endDateStr,
                                                   @QueryParam("include") String include,
                                                   @QueryParam("period") String period,
                                                   @QueryParam("metric") String metric,
                                                   @QueryParam("granularity") String granularity,
                                                   @QueryParam("confidence") Double confidence,
                                                   @QueryParam("days") Integer days,
                                                   @HeaderParam("Authorization") String authorization) {
        log.info("📊 StatsController.getSellerAnalyticsSummary() - SellerId: " + sellerId);
        log.info("📊 Desde: " + startDateStr + ", Hasta: " + endDateStr);
        log.info("📊 Include: " + include + ", Period: " + period + ", Metric: " + metric);
        log.info("📊 Granularity: " + granularity + ", Confidence: " + confidence + ", Days: " + days);
        
        // Validar parámetros de fecha
        LocalDate startDate, endDate;
        try {
            startDate = startDateStr != null ? LocalDate.parse(startDateStr, DATE_FORMATTER) : LocalDate.now().minusDays(30);
            endDate = endDateStr != null ? LocalDate.parse(endDateStr, DATE_FORMATTER) : LocalDate.now();
        } catch (DateTimeParseException e) {
            log.warn("❌ Fechas inválidas: " + e.getMessage());
            return Uni.createFrom().item(Response.status(400)
                    .entity(ApiResponse.error("Formato de fecha inválido. Use yyyy-MM-dd")).build());
        }
        
        // Validar autorización del vendedor
        return securityService.validateSellerAuthorization(authorization, sellerId)
                .chain(userId -> {
                    log.info("✅ Autorización exitosa para sellerId: " + sellerId);
                    return statsService.getSellerAnalyticsSummary(sellerId, startDate, endDate, include, period, metric, granularity, confidence, days);
                })
                .map(analytics -> {
                    log.info("✅ Analytics de vendedor obtenidos exitosamente");
                    return Response.ok(ApiResponse.success("Analytics de vendedor obtenidos exitosamente", analytics)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo analytics de vendedor: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    // ===== FINANCIAL & PAYMENT APIs - Para transparencia financiera =====
    
    @GET
    @Path("/admin/financial")
    @Operation(summary = "Get detailed financial analytics", 
               description = "Obtiene análisis financiero detallado con transparencia completa para administradores")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Análisis financiero obtenido exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado"),
        @APIResponse(responseCode = "400", description = "Parámetros inválidos")
    })
    public Uni<Response> getAdminFinancialAnalytics(@QueryParam("adminId") Long adminId,
                                                   @QueryParam("startDate") String startDateStr,
                                                   @QueryParam("endDate") String endDateStr,
                                                   @QueryParam("include") String include,
                                                   @QueryParam("currency") String currency,
                                                   @QueryParam("taxRate") Double taxRate,
                                                   @HeaderParam("Authorization") String authorization) {
        log.info("💰 StatsController.getAdminFinancialAnalytics() - AdminId: " + adminId);
        log.info("💰 Desde: " + startDateStr + ", Hasta: " + endDateStr);
        log.info("💰 Include: " + include + ", Currency: " + currency + ", TaxRate: " + taxRate);
        
        // Validar parámetros de fecha
        LocalDate startDate, endDate;
        try {
            startDate = startDateStr != null ? LocalDate.parse(startDateStr, DATE_FORMATTER) : LocalDate.now().minusDays(30);
            endDate = endDateStr != null ? LocalDate.parse(endDateStr, DATE_FORMATTER) : LocalDate.now();
        } catch (DateTimeParseException e) {
            log.warn("❌ Fechas inválidas: " + e.getMessage());
            return Uni.createFrom().item(Response.status(400)
                    .entity(ApiResponse.error("Formato de fecha inválido. Use yyyy-MM-dd")).build());
        }
        
        // Validar autorización de admin
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> {
                    log.info("✅ Autorización exitosa para adminId: " + adminId);
                    return statsService.getFinancialAnalytics(adminId, startDate, endDate, include, currency, taxRate);
                })
                .map(financial -> {
                    log.info("✅ Análisis financiero obtenido exitosamente");
                    return Response.ok(ApiResponse.success("Análisis financiero obtenido exitosamente", financial)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo análisis financiero: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    @GET
    @Path("/seller/financial")
    @Operation(summary = "Get seller financial analytics", 
               description = "Obtiene análisis financiero específico para vendedores con transparencia de comisiones")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Análisis financiero de vendedor obtenido exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado"),
        @APIResponse(responseCode = "400", description = "Parámetros inválidos")
    })
    public Uni<Response> getSellerFinancialAnalytics(@QueryParam("sellerId") Long sellerId,
                                                     @QueryParam("startDate") String startDateStr,
                                                     @QueryParam("endDate") String endDateStr,
                                                     @QueryParam("include") String include,
                                                     @QueryParam("currency") String currency,
                                                     @QueryParam("commissionRate") Double commissionRate,
                                                     @HeaderParam("Authorization") String authorization) {
        log.info("💰 StatsController.getSellerFinancialAnalytics() - SellerId: " + sellerId);
        log.info("💰 Desde: " + startDateStr + ", Hasta: " + endDateStr);
        log.info("💰 Include: " + include + ", Currency: " + currency + ", CommissionRate: " + commissionRate);
        
        // Validar parámetros de fecha
        LocalDate startDate, endDate;
        try {
            startDate = startDateStr != null ? LocalDate.parse(startDateStr, DATE_FORMATTER) : LocalDate.now().minusDays(30);
            endDate = endDateStr != null ? LocalDate.parse(endDateStr, DATE_FORMATTER) : LocalDate.now();
        } catch (DateTimeParseException e) {
            log.warn("❌ Fechas inválidas: " + e.getMessage());
            return Uni.createFrom().item(Response.status(400)
                    .entity(ApiResponse.error("Formato de fecha inválido. Use yyyy-MM-dd")).build());
        }
        
        // Validar autorización del vendedor
        return securityService.validateSellerAuthorization(authorization, sellerId)
                .chain(userId -> {
                    log.info("✅ Autorización exitosa para sellerId: " + sellerId);
                    return statsService.getSellerFinancialAnalytics(sellerId, startDate, endDate, include, currency, commissionRate);
                })
                .map(financial -> {
                    log.info("✅ Análisis financiero de vendedor obtenido exitosamente");
                    return Response.ok(ApiResponse.success("Análisis financiero de vendedor obtenido exitosamente", financial)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo análisis financiero de vendedor: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    @GET
    @Path("/admin/payment-transparency")
    @Operation(summary = "Get payment transparency report", 
               description = "Obtiene reporte de transparencia de pagos con detalles de comisiones, impuestos y fees")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Reporte de transparencia obtenido exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado"),
        @APIResponse(responseCode = "400", description = "Parámetros inválidos")
    })
    public Uni<Response> getPaymentTransparencyReport(@QueryParam("adminId") Long adminId,
                                                      @QueryParam("startDate") String startDateStr,
                                                      @QueryParam("endDate") String endDateStr,
                                                      @QueryParam("includeFees") Boolean includeFees,
                                                      @QueryParam("includeTaxes") Boolean includeTaxes,
                                                      @QueryParam("includeCommissions") Boolean includeCommissions,
                                                      @HeaderParam("Authorization") String authorization) {
        log.info("🔍 StatsController.getPaymentTransparencyReport() - AdminId: " + adminId);
        log.info("🔍 Desde: " + startDateStr + ", Hasta: " + endDateStr);
        log.info("🔍 IncludeFees: " + includeFees + ", IncludeTaxes: " + includeTaxes + ", IncludeCommissions: " + includeCommissions);
        
        // Validar parámetros de fecha
        LocalDate startDate, endDate;
        try {
            startDate = startDateStr != null ? LocalDate.parse(startDateStr, DATE_FORMATTER) : LocalDate.now().minusDays(30);
            endDate = endDateStr != null ? LocalDate.parse(endDateStr, DATE_FORMATTER) : LocalDate.now();
        } catch (DateTimeParseException e) {
            log.warn("❌ Fechas inválidas: " + e.getMessage());
            return Uni.createFrom().item(Response.status(400)
                    .entity(ApiResponse.error("Formato de fecha inválido. Use yyyy-MM-dd")).build());
        }
        
        // Validar autorización de admin
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> {
                    log.info("✅ Autorización exitosa para adminId: " + adminId);
                    return statsService.getPaymentTransparencyReport(adminId, startDate, endDate, includeFees, includeTaxes, includeCommissions);
                })
                .map(report -> {
                    log.info("✅ Reporte de transparencia obtenido exitosamente");
                    return Response.ok(ApiResponse.success("Reporte de transparencia obtenido exitosamente", report)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo reporte de transparencia: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
}
