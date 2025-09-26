package org.sky.controller;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.sky.dto.ApiResponse;
import org.sky.dto.billing.*;
import org.sky.service.ManualPaymentService;
import org.sky.service.SecurityService;
import org.sky.service.SubscriptionService;
import org.sky.service.TokenService;
import org.sky.repository.TokenPackageRepository;
import org.sky.repository.SubscriptionPlanRepository;
import org.sky.model.TokenPackage;
import org.sky.model.SubscriptionPlan;
import java.util.List;
import java.util.Map;

@Path("/api/billing")
@Tag(name = "Billing Management", description = "Gestión de facturación y tokens para usuarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BillingController {

    @Inject
    ManualPaymentService manualPaymentService;

    @Inject
    TokenService tokenService;

    @Inject
    SubscriptionService subscriptionService;

    @Inject
    SecurityService securityService;

    @Inject
    TokenPackageRepository tokenPackageRepository;

    @Inject
    SubscriptionPlanRepository subscriptionPlanRepository;

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
        Log.info("💰 BillingController.getBillingInfo() - Type: " + type + ", AdminId: " + adminId);
        
        // Para planes, no requerir autorización si adminId es null
        if ("plans".equals(type.toLowerCase()) && adminId == null) {
            return getAvailablePlans(include)
                    .map(response -> Response.ok(response).build());
        }
        
        // Validar autorización para otros tipos
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> {
                    return switch (type.toLowerCase()) {
                        case "tokens" -> getTokenStatus(adminId, period, include);
                        case "subscription" -> getSubscriptionStatus(adminId, period, include);
                        case "payments" -> getPaymentHistory(adminId, period, include);
                        case "dashboard" -> getDashboard(adminId, period, include);
                        case "plans" -> getAvailablePlans(include);
                        case "token-packages" -> getTokenPackages(include);
                        default -> Uni.createFrom().failure(new IllegalArgumentException("Tipo no válido: " + type));
                    };
                })
                .map(response -> Response.ok(response).build())
                .onFailure().recoverWithItem(throwable -> {
                    Log.warn("❌ Error obteniendo información de billing: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }

    private Uni<ApiResponse<Object>> getTokenStatus(Long adminId, String period, String include) {
        Log.info("🪙 BillingController.getTokenStatus() - AdminId: " + adminId);
        
        return tokenService.getTokenStatus(adminId)
                .map(tokenStatus -> {
                    Log.info("✅ Estado de tokens obtenido exitosamente");
                    return ApiResponse.success("Estado de tokens obtenido exitosamente", tokenStatus);
                });
    }

    private Uni<ApiResponse<Object>> getSubscriptionStatus(Long adminId, String period, String include) {
        Log.info("📋 BillingController.getSubscriptionStatus() - AdminId: " + adminId);
        
        return subscriptionService.getSubscriptionStatus(adminId)
                .map(subscriptionStatus -> {
                    Log.info("✅ Estado de suscripción obtenido exitosamente");
                    return ApiResponse.success("Estado de suscripción obtenido exitosamente", subscriptionStatus);
                });
    }

    private Uni<ApiResponse<Object>> getPaymentHistory(Long adminId, String period, String include) {
        Log.info("💳 BillingController.getPaymentHistory() - AdminId: " + adminId);
        
        return manualPaymentService.getPaymentHistory(adminId, period)
                .map(paymentHistory -> {
                    Log.info("✅ Historial de pagos obtenido exitosamente");
                    return ApiResponse.success("Historial de pagos obtenido exitosamente", paymentHistory);
                });
    }

    private Uni<ApiResponse<BillingDashboardResponse>> getDashboard(Long adminId, String period, String include) {
        Log.info("📊 BillingController.getDashboard() - AdminId: " + adminId);
        
        return tokenService.getTokenStatus(adminId)
                .chain(tokenStatus -> subscriptionService.getSubscriptionStatus(adminId)
                        .map(subscriptionStatus -> {
                    
                    // Dashboard simplificado sin historial de pagos por ahora
                    BillingDashboardResponse dashboard = new BillingDashboardResponse(
                            adminId,
                            tokenStatus,
                            subscriptionStatus,
                            List.of(), // Lista vacía de historial
                            new MonthlyUsageResponse(
                                    tokenStatus.tokensUsed().longValue(),
                                    tokenStatus.tokensAvailable().longValue(),
                                    0L, // Sin operaciones por ahora
                                    "N/A" // Sin operación más usada
                            ),
                            new BillingSummaryResponse(
                                    0.0, // Sin gastos por ahora
                                    "PEN",
                                    subscriptionStatus.endDate(),
                                    subscriptionStatus.isActive(),
                                    "manual"
                            ),
                            java.time.LocalDateTime.now()
                    );
                    
                            Log.info("✅ Dashboard de facturación obtenido exitosamente");
                            return ApiResponse.success("Dashboard obtenido exitosamente", dashboard);
                        }));
    }

    private Uni<ApiResponse<List<Map<String, Object>>>> getAvailablePlans(String include) {
        Log.info("📋 BillingController.getAvailablePlans()");
        
        return subscriptionPlanRepository.findAll()
                .list()
                .map(plans -> {
                    Log.info("🔍 Planes encontrados en BD: " + plans.size());
                    
                    List<Map<String, Object>> planList = plans.stream()
                            .map(plan -> {
                                Log.info("📋 Procesando plan: " + plan.name);
                                Map<String, Object> planMap = new java.util.HashMap<>();
                                planMap.put("id", plan.id);
                                planMap.put("name", plan.name);
                                planMap.put("description", plan.description != null ? plan.description : "");
                                planMap.put("price", plan.pricePen.doubleValue());
                                planMap.put("currency", "PEN");
                                planMap.put("billingCycle", plan.billingCycle);
                                planMap.put("maxAdmins", plan.maxAdmins);
                                planMap.put("maxSellers", plan.maxSellers);
                                planMap.put("tokensIncluded", plan.tokensIncluded);
                                planMap.put("features", parseFeatures(plan.features));
                                planMap.put("isActive", plan.isActive);
                                planMap.put("createdAt", plan.createdAt != null ? plan.createdAt.toString() : "");
                                return planMap;
                            })
                            .toList();
                    
                    Log.info("✅ Planes disponibles obtenidos exitosamente desde BD: " + plans.size() + " planes");
                    return ApiResponse.success("Planes obtenidos exitosamente", planList);
                })
                .onFailure().recoverWithItem(throwable -> {
                    Log.error("❌ Error obteniendo planes: " + throwable.getMessage());
                    throwable.printStackTrace();
                    return ApiResponse.error("Error obteniendo planes disponibles");
                });
    }

    private Uni<ApiResponse<List<Map<String, Object>>>> getTokenPackages(String include) {
        Log.info("🪙 BillingController.getTokenPackages()");
        
        return tokenPackageRepository.findActivePackages()
                .map(packages -> {
                    Log.info("🔍 Paquetes encontrados en BD: " + packages.size());
                    
                    List<Map<String, Object>> tokenPackages = packages.stream()
                            .map(pkg -> {
                                Log.info("📦 Procesando paquete: " + pkg.packageId + " - " + pkg.name);
                                return Map.of(
                                    "id", pkg.packageId,
                                    "name", pkg.name,
                                    "description", pkg.description != null ? pkg.description : "",
                                    "tokens", pkg.tokens,
                                    "price", pkg.price.doubleValue(),
                                    "currency", pkg.currency,
                                    "discount", pkg.discount.doubleValue(),
                                    "isPopular", pkg.isPopular,
                                    "features", parseFeatures(pkg.features),
                                    "discountedPrice", pkg.getDiscountedPrice().doubleValue()
                                );
                            })
                            .toList();
                    
                    Log.info("✅ Paquetes de tokens obtenidos exitosamente desde BD: " + packages.size() + " paquetes");
                    return ApiResponse.success("Paquetes de tokens obtenidos exitosamente", tokenPackages);
                })
                .onFailure().recoverWithItem(throwable -> {
                    Log.error("❌ Error obteniendo paquetes de tokens: " + throwable.getMessage());
                    throwable.printStackTrace();
                    return ApiResponse.error("Error obteniendo paquetes de tokens");
                });
    }
    
    
    
    /**
     * Parsea las características desde JSON string a List
     */
    private List<String> parseFeatures(String featuresJson) {
        if (featuresJson == null || featuresJson.trim().isEmpty()) {
            return List.of();
        }
        
        try {
            // Simple JSON parsing para el array de strings
            String cleaned = featuresJson.trim();
            if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
                cleaned = cleaned.substring(1, cleaned.length() - 1);
                return List.of(cleaned.split(","))
                        .stream()
                        .map(feature -> feature.trim().replaceAll("^\"|\"$", ""))
                        .toList();
            }
        } catch (Exception e) {
            Log.warn("⚠️ Error parseando características: " + e.getMessage());
        }
        
        return List.of();
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
        Log.info("🔧 BillingController.executeOperation() - Action: " + action + ", AdminId: " + adminId);
        
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> {
                    return switch (action.toLowerCase()) {
                        case "generate-code" -> executeGenerateCode(adminId, request, validate);
                        case "upload" -> executeUpload(adminId, request, validate);
                        case "subscribe" -> executeSubscribe(adminId, request, validate);
                        case "upgrade" -> executeUpgrade(adminId, request, validate);
                        case "cancel" -> executeCancel(adminId, request, validate);
                        case "purchase" -> executePurchase(adminId, request, validate);
                        case "check" -> executeCheck(adminId, request);
                        case "simulate" -> executeSimulate(adminId, request);
                        default -> Uni.createFrom().failure(new IllegalArgumentException("Acción no válida: " + action));
                    };
                })
                .map(response -> Response.ok(response).build())
                .onFailure().recoverWithItem(throwable -> {
                    Log.warn("❌ Error ejecutando operación: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }

    public Uni<ApiResponse<Object>> executeGenerateCode(Long adminId, PaymentRequest request, Boolean validate) {
        Log.info("💳 BillingController.executeGenerateCode() - AdminId: " + adminId);
        
        // Validar request
        if (!request.isValid()) {
            return Uni.createFrom().item(ApiResponse.error("Debe especificar planId para suscripción o tokensPackage para compra de tokens"));
        }
        
        return manualPaymentService.generatePaymentCode(adminId, request.planId(), request.tokensPackage())
                .map(paymentCode -> {
                    Log.info("✅ Código de pago generado exitosamente");
                    return ApiResponse.success("Código de pago generado exitosamente", paymentCode);
                });
    }

    private Uni<ApiResponse<Object>> executeUpload(Long adminId, PaymentRequest request, Boolean validate) {
        Log.info("📸 BillingController.executeUpload() - AdminId: " + adminId);
        
        if (request.paymentCode() == null || request.imageBase64() == null) {
            return Uni.createFrom().item(ApiResponse.error("paymentCode e imageBase64 son requeridos para subir imagen"));
        }
        
        return manualPaymentService.uploadPaymentImage(adminId, request.paymentCode(), request.imageBase64())
                .map(uploadResponse -> {
                    Log.info("✅ Imagen subida exitosamente");
                    return ApiResponse.success("Imagen subida exitosamente", uploadResponse);
                });
    }

    private Uni<ApiResponse<Object>> executeSubscribe(Long adminId, PaymentRequest request, Boolean validate) {
        Log.info("📋 BillingController.executeSubscribe() - AdminId: " + adminId);
        
        if (request.planId() == null) {
            return Uni.createFrom().item(ApiResponse.error("planId es requerido para suscripción"));
        }
        
        return subscriptionService.subscribeToPlan(adminId, request.planId())
                .map(subscriptionStatus -> {
                    Log.info("✅ Suscripción procesada exitosamente");
                    return ApiResponse.success("Suscripción procesada exitosamente", subscriptionStatus);
                });
    }

    private Uni<ApiResponse<Object>> executeUpgrade(Long adminId, PaymentRequest request, Boolean validate) {
        Log.info("⬆️ BillingController.executeUpgrade() - AdminId: " + adminId);
        
        if (request.planId() == null) {
            return Uni.createFrom().item(ApiResponse.error("planId es requerido para upgrade"));
        }
        
        return subscriptionService.upgradePlan(adminId, request.planId())
                .map(subscriptionStatus -> {
                    Log.info("✅ Upgrade procesado exitosamente");
                    return ApiResponse.success("Upgrade procesado exitosamente", subscriptionStatus);
                });
    }

    private Uni<ApiResponse<Object>> executeCancel(Long adminId, PaymentRequest request, Boolean validate) {
        Log.info("❌ BillingController.executeCancel() - AdminId: " + adminId);
        
        return subscriptionService.cancelSubscription(adminId)
                .map(subscriptionStatus -> {
                    Log.info("✅ Cancelación procesada exitosamente");
                    return ApiResponse.success("Cancelación procesada exitosamente", subscriptionStatus);
                });
    }

    private Uni<ApiResponse<Object>> executePurchase(Long adminId, PaymentRequest request, Boolean validate) {
        Log.info("🛒 BillingController.executePurchase() - AdminId: " + adminId);
        
        if (request.tokensPackage() == null) {
            return Uni.createFrom().item(ApiResponse.error("tokensPackage es requerido para compra"));
        }
        
        // Generar código de pago para compra de tokens
        return manualPaymentService.generatePaymentCode(adminId, null, request.tokensPackage())
                .map(paymentCode -> {
                    Log.info("✅ Código de pago para compra de tokens generado exitosamente");
                    return ApiResponse.success("Código de pago generado para compra de tokens", paymentCode);
                });
    }

    public Uni<ApiResponse<Object>> executeCheck(Long adminId, PaymentRequest request) {
        Log.info("🔍 BillingController.executeCheck() - AdminId: " + adminId);
        
        return Uni.combine().all()
                .unis(
                    tokenService.getTokenStatus(adminId),
                    subscriptionService.getSubscriptionStatus(adminId)
                )
                .asTuple()
                .map(tuple -> {
                    TokenStatusResponse tokenStatus = tuple.getItem1();
                    SubscriptionStatusResponse subscriptionStatus = tuple.getItem2();
                    
                    boolean hasTokens = tokenStatus.tokensAvailable() > 0;
                    boolean hasActiveSubscription = subscriptionStatus.isActive();
                    
                    String message = String.format("Tokens disponibles: %d, Suscripción activa: %s", 
                            tokenStatus.tokensAvailable(), hasActiveSubscription);
                    
                    Log.info("✅ Verificación de límites completada");
                    return ApiResponse.success("Verificación completada", Map.of(
                            "hasTokens", hasTokens,
                            "hasActiveSubscription", hasActiveSubscription,
                            "tokenStatus", tokenStatus,
                            "subscriptionStatus", subscriptionStatus,
                            "message", message
                    ));
                });
    }

    private Uni<ApiResponse<Object>> executeSimulate(Long adminId, PaymentRequest request) {
        Log.info("🎯 BillingController.executeSimulate() - AdminId: " + adminId);
        
        return Uni.combine().all()
                .unis(
                    tokenService.getTokenStatus(adminId),
                    subscriptionService.getSubscriptionStatus(adminId)
                )
                .asTuple()
                .map(tuple -> {
                    TokenStatusResponse tokenStatus = tuple.getItem1();
                    SubscriptionStatusResponse subscriptionStatus = tuple.getItem2();
                    
                    // Simular diferentes escenarios
                    Map<String, Object> simulation = Map.of(
                            "currentTokens", tokenStatus.tokensAvailable(),
                            "currentPlan", subscriptionStatus.planName(),
                            "simulatedOperations", List.of(
                                    Map.of("operation", "payment_processing", "tokensNeeded", 1, "canExecute", tokenStatus.tokensAvailable() >= 1),
                                    Map.of("operation", "qr_generation", "tokensNeeded", 1, "canExecute", tokenStatus.tokensAvailable() >= 1),
                                    Map.of("operation", "analytics_report", "tokensNeeded", 2, "canExecute", tokenStatus.tokensAvailable() >= 2)
                            ),
                            "recommendations", List.of(
                                    tokenStatus.tokensAvailable() < 10 ? "Considera comprar más tokens" : "Tienes suficientes tokens",
                                    !subscriptionStatus.isActive() ? "Considera activar una suscripción" : "Suscripción activa"
                            )
                    );
                    
                    Log.info("✅ Simulación completada");
                    return ApiResponse.success("Simulación completada", simulation);
                });
    }

    @POST
    @Path("/payments/upload")
    @Operation(summary = "Upload payment image", description = "Sube la imagen del comprobante de pago en formato base64")
    @WithSession
    public Uni<Response> uploadPaymentImage(@QueryParam("adminId") Long adminId,
                                          @QueryParam("paymentCode") String paymentCode,
                                          PaymentRequest request,
                                          @HeaderParam("Authorization") String authorization) {
        Log.info("📸 BillingController.uploadPaymentImage() - AdminId: " + adminId + ", Code: " + paymentCode);
        
        if (paymentCode == null || request.imageBase64() == null) {
            Log.warn("❌ Parámetros requeridos faltantes");
            return Uni.createFrom().item(Response.status(400)
                    .entity(ApiResponse.error("paymentCode e imageBase64 son requeridos")).build());
        }
        
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> {
                    Log.info("✅ Autorización exitosa para adminId: " + adminId);
                    return manualPaymentService.uploadPaymentImage(adminId, paymentCode, request.imageBase64());
                })
                .map(uploadResponse -> {
                    Log.info("✅ Imagen de pago subida exitosamente");
                    return Response.ok(ApiResponse.success("Imagen de pago subida exitosamente", uploadResponse)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    Log.warn("❌ Error subiendo imagen de pago: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }

    @GET
    @Path("/payments/status/{paymentCode}")
    @Operation(summary = "Get payment status", description = "Obtiene el estado de un pago por código")
    @WithSession
    public Uni<Response> getPaymentStatus(@PathParam("paymentCode") String paymentCode,
                                        @HeaderParam("Authorization") String authorization) {
        Log.info("🔍 BillingController.getPaymentStatus() - Code: " + paymentCode);
        
        return manualPaymentService.getPaymentStatus(paymentCode)
                .map(statusResponse -> {
                    Log.info("✅ Estado de pago obtenido exitosamente");
                    return Response.ok(ApiResponse.success("Estado de pago obtenido exitosamente", statusResponse)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    Log.warn("❌ Error obteniendo estado de pago: " + throwable.getMessage());
                    return Response.status(400)
                            .entity(ApiResponse.error("Error obteniendo estado de pago: " + throwable.getMessage())).build();
                });
    }

    @GET
    @Path("/plans")
    @Operation(summary = "Get available plans", description = "Obtiene los planes de suscripción disponibles")
    public Uni<Response> getAvailablePlans() {
        Log.info("📋 BillingController.getAvailablePlans()");
        
        // TODO: Implementar obtención de planes disponibles
        return Uni.createFrom().item(Response.ok(ApiResponse.success("Planes obtenidos exitosamente", "Planes disponibles")).build());
    }

    @GET
    @Path("/dashboard")
    @Operation(summary = "Get billing dashboard", description = "Obtiene el dashboard de facturación del administrador")
    public Uni<Response> getBillingDashboard(@QueryParam("adminId") Long adminId,
                                           @HeaderParam("Authorization") String authorization) {
        Log.info("📊 BillingController.getBillingDashboard() - AdminId: " + adminId);
        
        return securityService.validateAdminAuthorization(authorization, adminId)
                .chain(userId -> {
                    Log.info("✅ Autorización exitosa para adminId: " + adminId);
                    // TODO: Implementar dashboard completo
                    return tokenService.getTokenStatus(adminId);
                })
                .map(tokenStatus -> {
                    Log.info("✅ Dashboard de facturación obtenido exitosamente");
                    return Response.ok(ApiResponse.success("Dashboard obtenido exitosamente", tokenStatus)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    Log.warn("❌ Error obteniendo dashboard: " + throwable.getMessage());
                    return securityService.handleSecurityException(throwable);
                });
    }

    @POST
    @Path("/load-data")
    @Operation(summary = "Load subscription plans and token packages", description = "Carga los planes de suscripción y paquetes de tokens en la base de datos")
    @WithTransaction
    public Uni<Response> loadData() {
        Log.info("🔄 BillingController.loadData() - Cargando datos iniciales");
        
        // Crear Plan Básico
        SubscriptionPlan planBasico = new SubscriptionPlan();
        planBasico.name = "Plan Básico";
        planBasico.description = "Plan básico con funcionalidades esenciales para pequeñas empresas";
        planBasico.pricePen = java.math.BigDecimal.valueOf(50.00);
        planBasico.billingCycle = "monthly";
        planBasico.maxAdmins = 1;
        planBasico.maxSellers = 5;
        planBasico.tokensIncluded = 500;
        planBasico.features = "[\"QR Generation\", \"Payment Processing\", \"Basic Analytics\", \"Email Support\"]";
        planBasico.isActive = true;
        
        return subscriptionPlanRepository.persist(planBasico)
                .chain(savedPlan1 -> {
                    // Crear Plan Profesional
                    SubscriptionPlan planProfesional = new SubscriptionPlan();
                    planProfesional.name = "Plan Profesional";
                    planProfesional.description = "Plan profesional con funcionalidades avanzadas para empresas en crecimiento";
                    planProfesional.pricePen = java.math.BigDecimal.valueOf(150.00);
                    planProfesional.billingCycle = "monthly";
                    planProfesional.maxAdmins = 3;
                    planProfesional.maxSellers = 20;
                    planProfesional.tokensIncluded = 2000;
                    planProfesional.features = "[\"QR Generation\", \"Payment Processing\", \"Advanced Analytics\", \"Priority Support\", \"API Access\"]";
                    planProfesional.isActive = true;
                    
                    return subscriptionPlanRepository.persist(planProfesional);
                })
                .chain(savedPlan2 -> {
                    // Crear Plan Empresarial
                    SubscriptionPlan planEmpresarial = new SubscriptionPlan();
                    planEmpresarial.name = "Plan Empresarial";
                    planEmpresarial.description = "Plan empresarial con funcionalidades completas para grandes empresas";
                    planEmpresarial.pricePen = java.math.BigDecimal.valueOf(300.00);
                    planEmpresarial.billingCycle = "monthly";
                    planEmpresarial.maxAdmins = 10;
                    planEmpresarial.maxSellers = 100;
                    planEmpresarial.tokensIncluded = 10000;
                    planEmpresarial.features = "[\"QR Generation\", \"Payment Processing\", \"Advanced Analytics\", \"Priority Support\", \"API Access\", \"Custom Integrations\", \"Dedicated Support\"]";
                    planEmpresarial.isActive = true;
                    
                    return subscriptionPlanRepository.persist(planEmpresarial);
                })
                .map(savedPlan3 -> {
                    Log.info("✅ Planes creados exitosamente");
                    return Response.ok(ApiResponse.success("Planes creados exitosamente", Map.of(
                            "plansCreated", 3,
                            "message", "Plan Básico, Profesional y Empresarial creados"
                    ))).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    Log.error("❌ Error creando planes: " + throwable.getMessage());
                    return Response.status(500)
                            .entity(ApiResponse.error("Error creando planes: " + throwable.getMessage()))
                            .build();
                });
    }

    private Uni<Integer> loadSubscriptionPlans() {
        Log.info("📋 Cargando planes de suscripción");
        
        return subscriptionPlanRepository.findAll()
                .list()
                .chain(existingPlans -> {
                    if (!existingPlans.isEmpty()) {
                        Log.info("📋 Ya existen " + existingPlans.size() + " planes, limpiando...");
                        return subscriptionPlanRepository.deleteAll()
                                .replaceWith(0);
                    }
                    return Uni.createFrom().item(0);
                })
                .chain(deleted -> {
                    // Crear planes de suscripción uno por uno
                    SubscriptionPlan plan1 = createPlan("Plan Gratuito", "Plan básico gratuito con funcionalidades limitadas", 0.00, "monthly", 1, 2, 100, "[\"QR Generation\", \"Payment Processing\", \"Basic Analytics\"]");
                    SubscriptionPlan plan2 = createPlan("Plan Básico", "Plan básico con funcionalidades esenciales para pequeñas empresas", 50.00, "monthly", 1, 5, 500, "[\"QR Generation\", \"Payment Processing\", \"Basic Analytics\", \"Email Support\"]");
                    SubscriptionPlan plan3 = createPlan("Plan Profesional", "Plan profesional con funcionalidades avanzadas para empresas en crecimiento", 150.00, "monthly", 3, 20, 2000, "[\"QR Generation\", \"Payment Processing\", \"Advanced Analytics\", \"Priority Support\", \"API Access\"]");
                    SubscriptionPlan plan4 = createPlan("Plan Empresarial", "Plan empresarial con funcionalidades completas para grandes empresas", 300.00, "monthly", 10, 100, 10000, "[\"QR Generation\", \"Payment Processing\", \"Advanced Analytics\", \"Priority Support\", \"API Access\", \"Custom Integrations\", \"Dedicated Support\"]");
                    
                    return subscriptionPlanRepository.persist(plan1)
                            .chain(p1 -> subscriptionPlanRepository.persist(plan2))
                            .chain(p2 -> subscriptionPlanRepository.persist(plan3))
                            .chain(p3 -> subscriptionPlanRepository.persist(plan4))
                            .replaceWith(4);
                });
    }

    private Uni<Integer> loadTokenPackages() {
        Log.info("🪙 Cargando paquetes de tokens");
        
        return tokenPackageRepository.findAll()
                .list()
                .chain(existingPackages -> {
                    if (!existingPackages.isEmpty()) {
                        Log.info("🪙 Ya existen " + existingPackages.size() + " paquetes, limpiando...");
                        return tokenPackageRepository.deleteAll()
                                .replaceWith(0);
                    }
                    return Uni.createFrom().item(0);
                })
                .chain(deleted -> {
                    // Crear paquetes de tokens uno por uno
                    TokenPackage pkg1 = createTokenPackage("tokens_100", "Paquete Inicial", "100 tokens para empezar a probar el servicio", 100, 5.00, 0.0000, false, "[\"Procesamiento de pagos\", \"Generación de QR\", \"Reportes básicos\"]", 1);
                    TokenPackage pkg2 = createTokenPackage("tokens_500", "Paquete Básico", "500 tokens para uso básico mensual", 500, 18.00, 0.0000, true, "[\"Procesamiento de pagos\", \"Generación de QR\", \"Reportes básicos\", \"Soporte por email\"]", 2);
                    TokenPackage pkg3 = createTokenPackage("tokens_1000", "Paquete Estándar", "1000 tokens para uso moderado", 1000, 40.00, 0.1000, false, "[\"Procesamiento de pagos\", \"Generación de QR\", \"Reportes avanzados\", \"Soporte prioritario\"]", 3);
                    
                    return tokenPackageRepository.persist(pkg1)
                            .chain(p1 -> tokenPackageRepository.persist(pkg2))
                            .chain(p2 -> tokenPackageRepository.persist(pkg3))
                            .replaceWith(3);
                });
    }

    private SubscriptionPlan createPlan(String name, String description, double price, String billingCycle, 
                                      int maxAdmins, int maxSellers, int tokensIncluded, String features) {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.name = name;
        plan.description = description;
        plan.pricePen = java.math.BigDecimal.valueOf(price);
        plan.billingCycle = billingCycle;
        plan.maxAdmins = maxAdmins;
        plan.maxSellers = maxSellers;
        plan.tokensIncluded = tokensIncluded;
        plan.features = features;
        plan.isActive = true;
        return plan;
    }

    private TokenPackage createTokenPackage(String packageId, String name, String description, int tokens, 
                                          double price, double discount, boolean isPopular, String features, int sortOrder) {
        TokenPackage pkg = new TokenPackage();
        pkg.packageId = packageId;
        pkg.name = name;
        pkg.description = description;
        pkg.tokens = tokens;
        pkg.price = java.math.BigDecimal.valueOf(price);
        pkg.currency = "PEN";
        pkg.discount = java.math.BigDecimal.valueOf(discount);
        pkg.isPopular = isPopular;
        pkg.features = features;
        pkg.isActive = true;
        pkg.sortOrder = sortOrder;
        return pkg;
    }
}
