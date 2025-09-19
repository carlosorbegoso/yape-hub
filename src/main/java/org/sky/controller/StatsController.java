package org.sky.controller;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.sky.dto.ApiResponse;
import org.sky.dto.stats.SellerAnalyticsResponse;
import org.sky.service.SecurityService;
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
    @Path("/admin")
    @Operation(summary = "Get admin sales statistics", 
               description = "Obtiene estadísticas generales de ventas para un administrador")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Estadísticas obtenidas exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado"),
        @APIResponse(responseCode = "400", description = "Parámetros inválidos")
    })
    public Uni<Response> getAdminStats(@QueryParam("adminId") Long adminId,
                                       @QueryParam("startDate") String startDateStr,
                                       @QueryParam("endDate") String endDateStr,
                                       @HeaderParam("Authorization") String authorization) {
        log.info("📊 StatsController.getAdminStats() - AdminId: " + adminId);
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
                    log.info("✅ Estadísticas de admin obtenidas exitosamente");
                    return Response.ok(ApiResponse.success("Estadísticas de admin obtenidas exitosamente", stats)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo estadísticas de admin: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    @GET
    @Path("/seller")
    @Operation(summary = "Get seller sales statistics", 
               description = "Obtiene estadísticas específicas de ventas para un vendedor")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Estadísticas obtenidas exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado"),
        @APIResponse(responseCode = "400", description = "Parámetros inválidos"),
        @APIResponse(responseCode = "404", description = "Vendedor no encontrado")
    })
    public Uni<Response> getSellerStats(@QueryParam("sellerId") Long sellerId,
                                        @QueryParam("startDate") String startDateStr,
                                        @QueryParam("endDate") String endDateStr,
                                        @HeaderParam("Authorization") String authorization) {
        log.info("📊 StatsController.getSellerStats() - SellerId: " + sellerId);
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
                    log.info("✅ Estadísticas de vendedor obtenidas exitosamente");
                    return Response.ok(ApiResponse.success("Estadísticas de vendedor obtenidas exitosamente", stats)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo estadísticas de vendedor: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    @GET
    @Path("/admin/summary")
    @Operation(summary = "Get admin summary statistics", 
               description = "Obtiene un resumen rápido de estadísticas para el dashboard del admin")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Resumen obtenido exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado")
    })
    public Uni<Response> getAdminSummary(@QueryParam("adminId") Long adminId,
                                         @HeaderParam("Authorization") String authorization) {
        log.info("📊 StatsController.getAdminSummary() - AdminId: " + adminId);
        
        // Obtener estadísticas de los últimos 7 días
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(7);
        
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> {
                    log.info("✅ Autorización exitosa para adminId: " + adminId);
                    return statsService.getAdminStats(adminId, startDate, endDate);
                })
                .map(stats -> {
                    log.info("✅ Resumen de admin obtenido exitosamente");
                    return Response.ok(ApiResponse.success("Resumen de admin obtenido exitosamente", stats)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo resumen de admin: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    @GET
    @Path("/seller/summary")
    @Operation(summary = "Get seller summary statistics", 
               description = "Obtiene un resumen rápido de estadísticas para el dashboard del vendedor")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Resumen obtenido exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado")
    })
    public Uni<Response> getSellerSummary(@QueryParam("sellerId") Long sellerId,
                                          @HeaderParam("Authorization") String authorization) {
        log.info("📊 StatsController.getSellerSummary() - SellerId: " + sellerId);
        
        // Obtener estadísticas de los últimos 7 días
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(7);
        
        return securityService.validateSellerAuthorization(authorization, sellerId)
                .chain(userId -> {
                    log.info("✅ Autorización exitosa para sellerId: " + sellerId);
                    return statsService.getSellerStats(sellerId, startDate, endDate);
                })
                .map(stats -> {
                    log.info("✅ Resumen de vendedor obtenido exitosamente");
                    return Response.ok(ApiResponse.success("Resumen de vendedor obtenido exitosamente", stats)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo resumen de vendedor: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    @GET
    @Path("/analytics")
    @Operation(summary = "Get complete analytics summary", 
               description = "Obtiene resumen completo de analytics con ventas diarias, top vendedores y métricas de rendimiento")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Analytics obtenidos exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado"),
        @APIResponse(responseCode = "400", description = "Parámetros inválidos")
    })
    public Uni<Response> getAnalyticsSummary(@QueryParam("adminId") Long adminId,
                                            @QueryParam("startDate") String startDateStr,
                                            @QueryParam("endDate") String endDateStr,
                                            @HeaderParam("Authorization") String authorization) {
        log.info("📊 StatsController.getAnalyticsSummary() - AdminId: " + adminId);
        log.info("📊 Desde: " + startDateStr + ", Hasta: " + endDateStr);
        
        // Validar parámetros de fecha
        LocalDate startDate, endDate;
        try {
            startDate = startDateStr != null ? LocalDate.parse(startDateStr, DATE_FORMATTER) : LocalDate.now().minusDays(7);
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
                    return statsService.getAnalyticsSummary(adminId, startDate, endDate);
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
    @Path("/quick-summary")
    @Operation(summary = "Get quick summary for dashboard", 
               description = "Obtiene resumen rápido con métricas clave para el dashboard principal")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Resumen rápido obtenido exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado"),
        @APIResponse(responseCode = "400", description = "Parámetros inválidos")
    })
    public Uni<Response> getQuickSummary(@QueryParam("adminId") Long adminId,
                                         @QueryParam("startDate") String startDateStr,
                                         @QueryParam("endDate") String endDateStr,
                                         @HeaderParam("Authorization") String authorization) {
        log.info("📊 StatsController.getQuickSummary() - AdminId: " + adminId);
        log.info("📊 Desde: " + startDateStr + ", Hasta: " + endDateStr);
        
        // Validar parámetros de fecha
        LocalDate startDate, endDate;
        try {
            startDate = startDateStr != null ? LocalDate.parse(startDateStr, DATE_FORMATTER) : LocalDate.now().minusDays(7);
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
                    return statsService.getQuickSummary(adminId, startDate, endDate);
                })
                .map(summary -> {
                    log.info("✅ Resumen rápido obtenido exitosamente");
                    return Response.ok(ApiResponse.success("Resumen rápido obtenido exitosamente", summary)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo resumen rápido: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    @GET
    @Path("/seller/analytics")
    @Operation(summary = "Get seller analytics summary", 
               description = "Obtiene resumen completo de analytics para un vendedor específico")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Analytics de vendedor obtenidos exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado"),
        @APIResponse(responseCode = "400", description = "Parámetros inválidos")
    })
    public Uni<Response> getSellerAnalyticsSummary(@QueryParam("sellerId") Long sellerId,
                                                   @QueryParam("startDate") String startDateStr,
                                                   @QueryParam("endDate") String endDateStr,
                                                   @HeaderParam("Authorization") String authorization) {
        log.info("📊 StatsController.getSellerAnalyticsSummary() - SellerId: " + sellerId);
        log.info("📊 Desde: " + startDateStr + ", Hasta: " + endDateStr);
        
        // Validar parámetros de fecha
        LocalDate startDate, endDate;
        try {
            startDate = startDateStr != null ? LocalDate.parse(startDateStr, DATE_FORMATTER) : LocalDate.now().minusDays(7);
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
                    return statsService.getSellerAnalyticsSummary(sellerId, startDate, endDate);
                })
                .map(analytics -> {
                    log.info("✅ Analytics de vendedor obtenidos exitosamente");
                    return Response.ok(ApiResponse.<SellerAnalyticsResponse>success("Analytics de vendedor obtenidos exitosamente", analytics)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo analytics de vendedor: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
}
