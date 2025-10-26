package org.sky.controller;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
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
import java.util.Map;

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
    @Path("/summary")
    @Operation(summary = "Get unified statistics summary", 
               description = "Obtiene estadísticas básicas de ventas para admin o seller según el rol del usuario autenticado")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Estadísticas obtenidas exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado"),
        @APIResponse(responseCode = "400", description = "Parámetros inválidos"),
        @APIResponse(responseCode = "403", description = "Acceso denegado")
    })
    public Uni<Response> getUnifiedSummary(@QueryParam("adminId") Long adminId,
                                           @QueryParam("sellerId") Long sellerId,
                                           @QueryParam("startDate") String startDateStr,
                                           @QueryParam("endDate") String endDateStr,
                                           @HeaderParam("Authorization") String authorization) {
        log.info("📊 StatsController.getUnifiedSummary() - AdminId: " + adminId + ", SellerId: " + sellerId);
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
        
        // Validar JWT y obtener userId
        return securityService.validateJwtToken(authorization)
                .chain(userId -> {
                    log.info("✅ Token válido para userId: " + userId);
                    
                    // Determinar el rol del usuario y obtener estadísticas apropiadas
                    return statsService.getUserRole(userId)
                            .chain(userRole -> {
                                log.info("✅ Usuario " + userId + " tiene rol: " + userRole);
                                
                                if ("ADMIN".equals(userRole)) {
                                    // Admin: validar que adminId coincida con el usuario
                                    if (adminId == null || !adminId.equals(userId)) {
                                        log.warn("❌ Admin " + userId + " intentó acceder a adminId: " + adminId);
                                        return Uni.createFrom().failure(new SecurityException("Solo puedes acceder a tus propias estadísticas"));
                                    }
                                    
                                    log.info("📊 Obteniendo estadísticas de admin: " + adminId);
                                    return statsService.getAdminStats(adminId, startDate, endDate)
                                            .map(stats -> {
                                                Map<String, Object> result = new java.util.HashMap<>();
                                                result.put("userType", "ADMIN");
                                                result.put("userId", adminId);
                                                result.put("overview", stats.get("overview"));
                                                result.put("performanceMetrics", stats.get("performanceMetrics"));
                                                // Solo incluir topSellers si hay datos
                                                if (stats.get("topSellers") != null) {
                                                    result.put("topSellers", stats.get("topSellers"));
                                                }
                                                // URLs para datos detallados (topSellers ya incluido en respuesta directa)
                                                result.put("urls", Map.of(
                                                    "dailySales", "/api/stats/daily-sales?adminId=" + adminId + "&startDate=" + startDateStr + "&endDate=" + endDateStr,
                                                    "monthlySales", "/api/stats/monthly-sales?adminId=" + adminId + "&startDate=" + startDateStr + "&endDate=" + endDateStr,
                                                    "weeklySales", "/api/stats/weekly-sales?adminId=" + adminId + "&startDate=" + startDateStr + "&endDate=" + endDateStr,
                                                    "hourlySales", "/api/stats/hourly-sales?adminId=" + adminId + "&startDate=" + startDateStr + "&endDate=" + endDateStr,
                                                    "performanceDetails", "/api/stats/performance?adminId=" + adminId + "&startDate=" + startDateStr + "&endDate=" + endDateStr,
                                                    "completeAnalytics", "/api/stats/analytics?adminId=" + adminId + "&startDate=" + startDateStr + "&endDate=" + endDateStr
                                                ));
                                                return result;
                                            });
                                    
                                } else if ("SELLER".equals(userRole)) {
                                    // Seller: validar que sellerId coincida con el usuario
                                    if (sellerId == null) {
                                        log.warn("❌ Seller " + userId + " no proporcionó sellerId");
                                        return Uni.createFrom().failure(new IllegalArgumentException("sellerId es requerido para vendedores"));
                                    }
                                    
                                    // Verificar que el sellerId corresponde al usuario
                                    return statsService.validateSellerOwnership(userId, sellerId)
                                            .chain(valid -> {
                                                if (!valid) {
                                                    log.warn("❌ Seller " + userId + " intentó acceder a sellerId: " + sellerId);
                                                    return Uni.createFrom().failure(new SecurityException("Solo puedes acceder a tus propias estadísticas"));
                                                }
                                                
                                                log.info("📊 Obteniendo estadísticas de seller: " + sellerId);
                                                return statsService.getSellerStats(sellerId, startDate, endDate)
                                                        .map(stats -> {
                                                            Map<String, Object> result = new java.util.HashMap<>();
                                                            result.put("userType", "SELLER");
                                                            result.put("userId", sellerId);
                                                            result.put("overview", stats.get("overview"));
                                                            result.put("performanceMetrics", stats.get("performanceMetrics"));
                                                            // URLs para datos detallados
                                                            result.put("urls", Map.of(
                                                                "dailySales", "/api/stats/daily-sales?sellerId=" + sellerId + "&startDate=" + startDateStr + "&endDate=" + endDateStr,
                                                                "monthlySales", "/api/stats/monthly-sales?sellerId=" + sellerId + "&startDate=" + startDateStr + "&endDate=" + endDateStr,
                                                                "weeklySales", "/api/stats/weekly-sales?sellerId=" + sellerId + "&startDate=" + startDateStr + "&endDate=" + endDateStr,
                                                                "hourlySales", "/api/stats/hourly-sales?sellerId=" + sellerId + "&startDate=" + startDateStr + "&endDate=" + endDateStr,
                                                                "performanceDetails", "/api/stats/performance?sellerId=" + sellerId + "&startDate=" + startDateStr + "&endDate=" + endDateStr
                                                            ));
                                                            return result;
                                                        });
                                            });
                                    
                                } else {
                                    log.error("❌ Rol de usuario no válido: " + userRole);
                                    return Uni.createFrom().failure(new SecurityException("Rol de usuario no válido: " + userRole));
                                }
                            });
                })
                .map(stats -> {
                    log.info("✅ Estadísticas unificadas obtenidas exitosamente");
                    return Response.ok(ApiResponse.success("Estadísticas obtenidas exitosamente", stats)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo estadísticas: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    
    // ===== DETAILED ENDPOINTS REFERENCED IN URLS =====
    
    @GET
    @Path("/performance")
    @Operation(summary = "Get performance metrics", 
               description = "Obtiene métricas de rendimiento detalladas para admin o seller")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Métricas de rendimiento obtenidas exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado"),
        @APIResponse(responseCode = "400", description = "Parámetros inválidos")
    })
    public Uni<Response> getPerformanceMetrics(@QueryParam("adminId") Long adminId,
                                               @QueryParam("sellerId") Long sellerId,
                                               @QueryParam("startDate") String startDateStr,
                                               @QueryParam("endDate") String endDateStr,
                                               @HeaderParam("Authorization") String authorization) {
        log.info("📊 StatsController.getPerformanceMetrics() - AdminId: " + adminId + ", SellerId: " + sellerId);
        
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
        
        return securityService.validateJwtToken(authorization)
                .chain(userId -> {
                    log.info("✅ Token válido para userId: " + userId);
                    
                    return statsService.getUserRole(userId)
                            .chain(userRole -> {
                                log.info("✅ Usuario " + userId + " tiene rol: " + userRole);
                                
                                if ("ADMIN".equals(userRole)) {
                                    if (adminId == null || !adminId.equals(userId)) {
                                        return Uni.createFrom().failure(new SecurityException("Solo puedes acceder a tus propias estadísticas"));
                                    }
                                    
                                    return statsService.getAdminStats(adminId, startDate, endDate)
                                            .map(stats -> {
                                                Map<String, Object> result = new java.util.HashMap<>();
                                                result.put("userType", "ADMIN");
                                                result.put("userId", adminId);
                                                result.put("performanceMetrics", stats.get("performanceMetrics"));
                                                return result;
                                            });
                                    
                                } else if ("SELLER".equals(userRole)) {
                                    if (sellerId == null) {
                                        return Uni.createFrom().failure(new IllegalArgumentException("sellerId es requerido para vendedores"));
                                    }
                                    
                                    return statsService.validateSellerOwnership(userId, sellerId)
                                            .chain(valid -> {
                                                if (!valid) {
                                                    return Uni.createFrom().failure(new SecurityException("Solo puedes acceder a tus propias estadísticas"));
                                                }
                                                
                                                return statsService.getSellerStats(sellerId, startDate, endDate)
                                                        .map(stats -> {
                                                            Map<String, Object> result = new java.util.HashMap<>();
                                                            result.put("userType", "SELLER");
                                                            result.put("userId", sellerId);
                                                            result.put("performanceMetrics", stats.get("performanceMetrics"));
                                                            return result;
                                                        });
                                            });
                                    
                                } else {
                                    return Uni.createFrom().failure(new SecurityException("Rol de usuario no válido: " + userRole));
                                }
                            });
                })
                .map(metrics -> {
                    log.info("✅ Métricas de rendimiento obtenidas exitosamente");
                    return Response.ok(ApiResponse.success("Métricas de rendimiento obtenidas exitosamente", metrics)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo métricas de rendimiento: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    @GET
    @Path("/daily-sales")
    @Operation(summary = "Get daily sales data", 
               description = "Obtiene datos de ventas diarias para admin o seller")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Datos de ventas diarias obtenidos exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado"),
        @APIResponse(responseCode = "400", description = "Parámetros inválidos")
    })
    public Uni<Response> getDailySales(@QueryParam("adminId") Long adminId,
                                       @QueryParam("sellerId") Long sellerId,
                                       @QueryParam("startDate") String startDateStr,
                                       @QueryParam("endDate") String endDateStr,
                                       @HeaderParam("Authorization") String authorization) {
        log.info("📊 StatsController.getDailySales() - AdminId: " + adminId + ", SellerId: " + sellerId);
        
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
        
        return securityService.validateJwtToken(authorization)
                .chain(userId -> {
                    log.info("✅ Token válido para userId: " + userId);
                    
                    return statsService.getUserRole(userId)
                            .chain(userRole -> {
                                log.info("✅ Usuario " + userId + " tiene rol: " + userRole);
                                
                                if ("ADMIN".equals(userRole)) {
                                    if (adminId == null || !adminId.equals(userId)) {
                                        return Uni.createFrom().failure(new SecurityException("Solo puedes acceder a tus propias estadísticas"));
                                    }
                                    
                                    return statsService.getAdminStats(adminId, startDate, endDate)
                                            .map(stats -> {
                                                Map<String, Object> result = new java.util.HashMap<>();
                                                result.put("userType", "ADMIN");
                                                result.put("userId", adminId);
                                                result.put("dailySales", stats.get("dailySales"));
                                                return result;
                                            });
                                    
                                } else if ("SELLER".equals(userRole)) {
                                    if (sellerId == null) {
                                        return Uni.createFrom().failure(new IllegalArgumentException("sellerId es requerido para vendedores"));
                                    }
                                    
                                    return statsService.validateSellerOwnership(userId, sellerId)
                                            .chain(valid -> {
                                                if (!valid) {
                                                    return Uni.createFrom().failure(new SecurityException("Solo puedes acceder a tus propias estadísticas"));
                                                }
                                                
                                                return statsService.getSellerStats(sellerId, startDate, endDate)
                                                        .map(stats -> {
                                                            Map<String, Object> result = new java.util.HashMap<>();
                                                            result.put("userType", "SELLER");
                                                            result.put("userId", sellerId);
                                                            result.put("dailySales", stats.get("dailySales"));
                                                            return result;
                                                        });
                                            });
                                    
                                } else {
                                    return Uni.createFrom().failure(new SecurityException("Rol de usuario no válido: " + userRole));
                                }
                            });
                })
                .map(sales -> {
                    log.info("✅ Datos de ventas diarias obtenidos exitosamente");
                    return Response.ok(ApiResponse.success("Datos de ventas diarias obtenidos exitosamente", sales)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo datos de ventas diarias: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    @GET
    @Path("/monthly-sales")
    @Operation(summary = "Get monthly sales data", 
               description = "Obtiene datos de ventas mensuales para admin o seller")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Datos de ventas mensuales obtenidos exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado"),
        @APIResponse(responseCode = "400", description = "Parámetros inválidos")
    })
    public Uni<Response> getMonthlySales(@QueryParam("adminId") Long adminId,
                                         @QueryParam("sellerId") Long sellerId,
                                         @QueryParam("startDate") String startDateStr,
                                         @QueryParam("endDate") String endDateStr,
                                         @HeaderParam("Authorization") String authorization) {
        log.info("📊 StatsController.getMonthlySales() - AdminId: " + adminId + ", SellerId: " + sellerId);
        
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
        
        return securityService.validateJwtToken(authorization)
                .chain(userId -> {
                    log.info("✅ Token válido para userId: " + userId);
                    
                    return statsService.getUserRole(userId)
                            .chain(userRole -> {
                                log.info("✅ Usuario " + userId + " tiene rol: " + userRole);
                                
                                if ("ADMIN".equals(userRole)) {
                                    if (adminId == null || !adminId.equals(userId)) {
                                        return Uni.createFrom().failure(new SecurityException("Solo puedes acceder a tus propias estadísticas"));
                                    }
                                    
                                    return statsService.getAdminAnalytics(adminId, startDate, endDate)
                                            .map(analytics -> {
                                                Map<String, Object> result = new java.util.HashMap<>();
                                                result.put("userType", "ADMIN");
                                                result.put("userId", adminId);
                                                result.put("monthlySales", analytics.monthlySales());
                                                return result;
                                            });
                                    
                                } else if ("SELLER".equals(userRole)) {
                                    if (sellerId == null) {
                                        return Uni.createFrom().failure(new IllegalArgumentException("sellerId es requerido para vendedores"));
                                    }
                                    
                                    return statsService.validateSellerOwnership(userId, sellerId)
                                            .chain(valid -> {
                                                if (!valid) {
                                                    return Uni.createFrom().failure(new SecurityException("Solo puedes acceder a tus propias estadísticas"));
                                                }
                                                
                                                return statsService.getAnalyticsSummary(sellerId, startDate, endDate, null, null, null, null, null, null)
                                                        .map(analytics -> {
                                                            Map<String, Object> result = new java.util.HashMap<>();
                                                            result.put("userType", "SELLER");
                                                            result.put("userId", sellerId);
                                                            result.put("monthlySales", analytics.monthlySales());
                                                            return result;
                                                        });
                                            });
                                    
                                } else {
                                    return Uni.createFrom().failure(new SecurityException("Rol de usuario no válido: " + userRole));
                                }
                            });
                })
                .map(sales -> {
                    log.info("✅ Datos de ventas mensuales obtenidos exitosamente");
                    return Response.ok(ApiResponse.success("Datos de ventas mensuales obtenidos exitosamente", sales)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo datos de ventas mensuales: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    @GET
    @Path("/top-sellers")
    @Operation(summary = "Get top sellers data", 
               description = "Obtiene datos de los mejores vendedores para admin")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Datos de top vendedores obtenidos exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado"),
        @APIResponse(responseCode = "400", description = "Parámetros inválidos")
    })
    public Uni<Response> getTopSellers(@QueryParam("adminId") Long adminId,
                                       @QueryParam("startDate") String startDateStr,
                                       @QueryParam("endDate") String endDateStr,
                                       @HeaderParam("Authorization") String authorization) {
        log.info("📊 StatsController.getTopSellers() - AdminId: " + adminId);
        
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
        
        return securityService.validateJwtToken(authorization)
                .chain(userId -> {
                    log.info("✅ Token válido para userId: " + userId);
                    
                    return statsService.getUserRole(userId)
                            .chain(userRole -> {
                                log.info("✅ Usuario " + userId + " tiene rol: " + userRole);
                                
                                if ("ADMIN".equals(userRole)) {
                                    if (adminId == null || !adminId.equals(userId)) {
                                        return Uni.createFrom().failure(new SecurityException("Solo puedes acceder a tus propias estadísticas"));
                                    }
                                    
                                    return statsService.getAdminStats(adminId, startDate, endDate)
                                            .map(stats -> {
                                                Map<String, Object> result = new java.util.HashMap<>();
                                                result.put("userType", "ADMIN");
                                                result.put("userId", adminId);
                                                result.put("topSellers", stats.get("topSellers"));
                                                return result;
                                            });
                                    
                                } else {
                                    return Uni.createFrom().failure(new SecurityException("Solo los administradores pueden acceder a datos de top vendedores"));
                                }
                            });
                })
                .map(sellers -> {
                    log.info("✅ Datos de top vendedores obtenidos exitosamente");
                    return Response.ok(ApiResponse.success("Datos de top vendedores obtenidos exitosamente", sellers)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo datos de top vendedores: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    @GET
    @Path("/hourly-sales")
    @Operation(summary = "Get hourly sales data", 
               description = "Obtiene datos de ventas por hora para admin o seller")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Datos de ventas por hora obtenidos exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado"),
        @APIResponse(responseCode = "400", description = "Parámetros inválidos")
    })
    public Uni<Response> getHourlySales(@QueryParam("adminId") Long adminId,
                                        @QueryParam("sellerId") Long sellerId,
                                        @QueryParam("startDate") String startDateStr,
                                        @QueryParam("endDate") String endDateStr,
                                        @HeaderParam("Authorization") String authorization) {
        log.info("📊 StatsController.getHourlySales() - AdminId: " + adminId + ", SellerId: " + sellerId);
        
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
        
        return securityService.validateJwtToken(authorization)
                .chain(userId -> {
                    log.info("✅ Token válido para userId: " + userId);
                    
                    return statsService.getUserRole(userId)
                            .chain(userRole -> {
                                log.info("✅ Usuario " + userId + " tiene rol: " + userRole);
                                
                                if ("ADMIN".equals(userRole)) {
                                    if (adminId == null || !adminId.equals(userId)) {
                                        return Uni.createFrom().failure(new SecurityException("Solo puedes acceder a tus propias estadísticas"));
                                    }
                                    
                                    return statsService.getAdminStats(adminId, startDate, endDate)
                                            .map(stats -> {
                                                Map<String, Object> result = new java.util.HashMap<>();
                                                result.put("userType", "ADMIN");
                                                result.put("userId", adminId);
                                                result.put("hourlySales", stats.get("hourlySales"));
                                                return result;
                                            });
                                    
                                } else if ("SELLER".equals(userRole)) {
                                    if (sellerId == null) {
                                        return Uni.createFrom().failure(new IllegalArgumentException("sellerId es requerido para vendedores"));
                                    }
                                    
                                    return statsService.validateSellerOwnership(userId, sellerId)
                                            .chain(valid -> {
                                                if (!valid) {
                                                    return Uni.createFrom().failure(new SecurityException("Solo puedes acceder a tus propias estadísticas"));
                                                }
                                                
                                                return statsService.getSellerStats(sellerId, startDate, endDate)
                                                        .map(stats -> {
                                                            Map<String, Object> result = new java.util.HashMap<>();
                                                            result.put("userType", "SELLER");
                                                            result.put("userId", sellerId);
                                                            result.put("hourlySales", stats.get("hourlySales"));
                                                            return result;
                                                        });
                                            });
                                    
                                } else {
                                    return Uni.createFrom().failure(new SecurityException("Rol de usuario no válido: " + userRole));
                                }
                            });
                })
                .map(sales -> {
                    log.info("✅ Datos de ventas por hora obtenidos exitosamente");
                    return Response.ok(ApiResponse.success("Datos de ventas por hora obtenidos exitosamente", sales)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo datos de ventas por hora: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    @GET
    @Path("/weekly-sales")
    @Operation(summary = "Get weekly sales data", 
               description = "Obtiene datos de ventas semanales para admin o seller")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Datos de ventas semanales obtenidos exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado"),
        @APIResponse(responseCode = "400", description = "Parámetros inválidos")
    })
    public Uni<Response> getWeeklySales(@QueryParam("adminId") Long adminId,
                                        @QueryParam("sellerId") Long sellerId,
                                        @QueryParam("startDate") String startDateStr,
                                        @QueryParam("endDate") String endDateStr,
                                        @HeaderParam("Authorization") String authorization) {
        log.info("📊 StatsController.getWeeklySales() - AdminId: " + adminId + ", SellerId: " + sellerId);
        
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
        
        return securityService.validateJwtToken(authorization)
                .chain(userId -> {
                    log.info("✅ Token válido para userId: " + userId);
                    
                    return statsService.getUserRole(userId)
                            .chain(userRole -> {
                                log.info("✅ Usuario " + userId + " tiene rol: " + userRole);
                                
                                if ("ADMIN".equals(userRole)) {
                                    if (adminId == null || !adminId.equals(userId)) {
                                        return Uni.createFrom().failure(new SecurityException("Solo puedes acceder a tus propias estadísticas"));
                                    }
                                    
                                    return statsService.getAdminStats(adminId, startDate, endDate)
                                            .map(stats -> {
                                                Map<String, Object> result = new java.util.HashMap<>();
                                                result.put("userType", "ADMIN");
                                                result.put("userId", adminId);
                                                result.put("weeklySales", stats.get("weeklySales"));
                                                return result;
                                            });
                                    
                                } else if ("SELLER".equals(userRole)) {
                                    if (sellerId == null) {
                                        return Uni.createFrom().failure(new IllegalArgumentException("sellerId es requerido para vendedores"));
                                    }
                                    
                                    return statsService.validateSellerOwnership(userId, sellerId)
                                            .chain(valid -> {
                                                if (!valid) {
                                                    return Uni.createFrom().failure(new SecurityException("Solo puedes acceder a tus propias estadísticas"));
                                                }
                                                
                                                return statsService.getSellerStats(sellerId, startDate, endDate)
                                                        .map(stats -> {
                                                            Map<String, Object> result = new java.util.HashMap<>();
                                                            result.put("userType", "SELLER");
                                                            result.put("userId", sellerId);
                                                            result.put("weeklySales", stats.get("weeklySales"));
                                                            return result;
                                                        });
                                            });
                                    
                                } else {
                                    return Uni.createFrom().failure(new SecurityException("Rol de usuario no válido: " + userRole));
                                }
                            });
                })
                .map(sales -> {
                    log.info("✅ Datos de ventas semanales obtenidos exitosamente");
                    return Response.ok(ApiResponse.success("Datos de ventas semanales obtenidos exitosamente", sales)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo datos de ventas semanales: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
    
    @GET
    @Path("/analytics")
    @Operation(summary = "Get complete analytics data", 
               description = "Obtiene analytics completos para admin (incluye todos los datos detallados)")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Analytics completos obtenidos exitosamente"),
        @APIResponse(responseCode = "401", description = "No autorizado"),
        @APIResponse(responseCode = "400", description = "Parámetros inválidos"),
        @APIResponse(responseCode = "403", description = "Solo administradores pueden acceder")
    })
    public Uni<Response> getCompleteAnalytics(@QueryParam("adminId") Long adminId,
                                               @QueryParam("startDate") String startDateStr,
                                               @QueryParam("endDate") String endDateStr,
                                               @QueryParam("include") String include,
                                               @QueryParam("period") String period,
                                               @QueryParam("metric") String metric,
                                               @QueryParam("granularity") String granularity,
                                               @QueryParam("confidence") Double confidence,
                                               @QueryParam("days") Integer days,
                                               @HeaderParam("Authorization") String authorization) {
        log.info("📊 StatsController.getCompleteAnalytics() - AdminId: " + adminId);
        
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
        
        return securityService.validateJwtToken(authorization)
                .chain(userId -> {
                    log.info("✅ Token válido para userId: " + userId);
                    
                    return statsService.getUserRole(userId)
                            .chain(userRole -> {
                                log.info("✅ Usuario " + userId + " tiene rol: " + userRole);
                                
                                if ("ADMIN".equals(userRole)) {
                                    if (adminId == null || !adminId.equals(userId)) {
                                        return Uni.createFrom().failure(new SecurityException("Solo puedes acceder a tus propias estadísticas"));
                                    }
                                    
                                    return statsService.getAnalyticsSummary(adminId, startDate, endDate, 
                                                                           include, period, metric, granularity, confidence, days)
                                            .map(analytics -> {
                                                Map<String, Object> result = new java.util.HashMap<>();
                                                result.put("userType", "ADMIN");
                                                result.put("userId", adminId);
                                                result.put("analytics", analytics);
                                                return result;
                                            });
                                    
                                } else {
                                    return Uni.createFrom().failure(new SecurityException("Solo los administradores pueden acceder a analytics completos"));
                                }
                            });
                })
                .map(analytics -> {
                    log.info("✅ Analytics completos obtenidos exitosamente");
                    return Response.ok(ApiResponse.success("Analytics completos obtenidos exitosamente", analytics)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("❌ Error obteniendo analytics completos: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }
}
