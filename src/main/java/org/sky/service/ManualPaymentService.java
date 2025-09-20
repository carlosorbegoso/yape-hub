package org.sky.service;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.sky.dto.billing.PaymentCodeResponse;
import org.sky.dto.billing.PaymentHistoryResponse;
import org.sky.dto.billing.PaymentStatusResponse;
import org.sky.dto.billing.PaymentUploadResponse;
import org.sky.model.PaymentCode;
import org.sky.model.ManualPayment;
import org.sky.model.PaymentHistory;
import org.sky.repository.PaymentCodeRepository;
import org.sky.repository.ManualPaymentRepository;
import org.sky.repository.PaymentHistoryRepository;
import org.sky.repository.SubscriptionPlanRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ManualPaymentService {

    private static final Logger log = Logger.getLogger(ManualPaymentService.class);

    @Inject
    PaymentCodeRepository paymentCodeRepository;

    @Inject
    ManualPaymentRepository manualPaymentRepository;

    @Inject
    PaymentHistoryRepository paymentHistoryRepository;

    @Inject
    SubscriptionPlanRepository subscriptionPlanRepository;

    @Inject
    TokenService tokenService;

    // Número Yape asignado para pagos (configurar en application.properties)
    private static final String YAPE_NUMBER = "977737772";

    @WithTransaction
    public Uni<PaymentCodeResponse> generatePaymentCode(Long adminId, Long planId, String tokensPackage) {
        log.info("💳 ManualPaymentService.generatePaymentCode() - AdminId: " + adminId + ", PlanId: " + planId + ", TokensPackage: " + tokensPackage);
        
        // Generar código único
        String paymentCode = generateUniqueCode();
        
        // Calcular monto de forma asíncrona
        return calculateAmount(planId, tokensPackage)
                .chain(amount -> {
        
        // Crear código de pago
        PaymentCode paymentCodeEntity = new PaymentCode();
        paymentCodeEntity.code = paymentCode;
        paymentCodeEntity.adminId = adminId;
        paymentCodeEntity.planId = planId;
        paymentCodeEntity.tokensPackage = tokensPackage;
        paymentCodeEntity.amountPen = BigDecimal.valueOf(amount);
        paymentCodeEntity.yapeNumber = YAPE_NUMBER;
        paymentCodeEntity.status = "pending";
        paymentCodeEntity.expiresAt = LocalDateTime.now().plusHours(24);
        
                    return paymentCodeRepository.persist(paymentCodeEntity)
                            .map(savedCode -> {
                                log.info("✅ Código de pago generado: " + paymentCode);
                                return new PaymentCodeResponse(
                                    paymentCode,
                                    YAPE_NUMBER,
                                    amount,
                                    "PEN",
                                    savedCode.expiresAt,
                                    "Realiza el pago y sube la captura de pantalla"
                                );
                            });
                });
    }

    @WithTransaction
    public Uni<PaymentUploadResponse> uploadPaymentImage(Long adminId, String paymentCode, String imageBase64) {
        log.info("📸 ManualPaymentService.uploadPaymentImage() - AdminId: " + adminId + ", Code: " + paymentCode);
        
        // Validar formato base64
        if (!isValidBase64(imageBase64)) {
            return Uni.createFrom().failure(new RuntimeException("Formato de imagen base64 inválido"));
        }
        
        // Verificar que el código existe y está pendiente
        return paymentCodeRepository.findValidCode(paymentCode)
                .chain(code -> {
                    if (code == null) {
                        return Uni.createFrom().failure(new RuntimeException("Código de pago inválido o expirado"));
                    }
                    
                    // Crear registro de pago manual
                    ManualPayment payment = new ManualPayment();
                    payment.paymentCodeId = code.id;
                    payment.adminId = adminId;
                    payment.imageBase64 = imageBase64;
                    payment.amountPen = code.amountPen;
                    payment.yapeNumber = code.yapeNumber;
                    payment.status = "pending";
                    
                    return manualPaymentRepository.persist(payment)
                            .map(savedPayment -> {
                                log.info("✅ Imagen de pago subida en base64 para código: " + paymentCode);
                                return new PaymentUploadResponse(
                                    savedPayment.id,
                                    "Imagen subida exitosamente. Esperando revisión del administrador.",
                                    "pending"
                                );
                            });
                });
    }

    @WithTransaction
    public Uni<PaymentStatusResponse> getPaymentStatus(String paymentCode) {
        log.info("🔍 ManualPaymentService.getPaymentStatus() - Code: " + paymentCode);
        
        return paymentCodeRepository.findByCode(paymentCode)
                .map(code -> {
                    if (code == null) {
                        return new PaymentStatusResponse(
                            paymentCode,
                            "not_found",
                            0.0,
                            "PEN",
                            null,
                            false,
                            "Código de pago no encontrado"
                        );
                    }
                    
                    boolean isExpired = code.isExpired();
                    String status = isExpired ? "expired" : code.status;
                    String message = getStatusMessage(status, isExpired);
                    
                    return new PaymentStatusResponse(
                        paymentCode,
                        status,
                        code.amountPen.doubleValue(),
                        "PEN",
                        code.expiresAt,
                        isExpired,
                        message
                    );
                });
    }

    @WithTransaction
    public Uni<PaymentUploadResponse> approvePayment(Long adminId, Long paymentId, String reviewNotes) {
        log.info("✅ ManualPaymentService.approvePayment() - AdminId: " + adminId + ", PaymentId: " + paymentId);
        
        return manualPaymentRepository.findById(paymentId)
                .chain(payment -> {
                    if (payment == null || !payment.isPending()) {
                        return Uni.createFrom().failure(new RuntimeException("Pago no encontrado o ya procesado"));
                    }
                    
                    // Aprobar el pago
                    payment.approve(adminId, reviewNotes);
                    
                    return manualPaymentRepository.persist(payment)
                            .chain(savedPayment -> {
                                // Actualizar código de pago
                                return paymentCodeRepository.findById(payment.paymentCodeId)
                                        .chain(code -> {
                                            code.markAsPaid();
                                            return paymentCodeRepository.persist(code);
                                        })
                                        .chain(updatedCode -> {
                                            // Activar plan/tokens para el admin
                                            return activatePlanOrTokens(payment.adminId, updatedCode)
                                                    .map(activation -> {
                                                        log.info("✅ Pago aprobado y plan activado para admin " + payment.adminId);
                                                        return new PaymentUploadResponse(
                                                            paymentId,
                                                            "Pago aprobado exitosamente",
                                                            "approved"
                                                        );
                                                    });
                                        });
                            });
                });
    }

    @WithTransaction
    public Uni<PaymentUploadResponse> rejectPayment(Long adminId, Long paymentId, String reviewNotes) {
        log.info("❌ ManualPaymentService.rejectPayment() - AdminId: " + adminId + ", PaymentId: " + paymentId);
        
        return manualPaymentRepository.findById(paymentId)
                .chain(payment -> {
                    if (payment == null || !payment.isPending()) {
                        return Uni.createFrom().failure(new RuntimeException("Pago no encontrado o ya procesado"));
                    }
                    
                    // Rechazar el pago
                    payment.reject(adminId, reviewNotes);
                    
                    return manualPaymentRepository.persist(payment)
                            .map(savedPayment -> {
                                log.info("❌ Pago rechazado para admin " + payment.adminId);
                                return new PaymentUploadResponse(
                                    paymentId,
                                    "Pago rechazado: " + reviewNotes,
                                    "rejected"
                                );
                            });
                });
    }

    private String generateUniqueCode() {
        return "PAY_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private boolean isValidBase64(String base64String) {
        if (base64String == null || base64String.trim().isEmpty()) {
            return false;
        }
        
        try {
            // Verificar que sea base64 válido
            java.util.Base64.getDecoder().decode(base64String);
            
            // Verificar que tenga el prefijo de imagen (data:image/...)
            if (!base64String.startsWith("data:image/")) {
                return false;
            }
            
            // Verificar que tenga el formato correcto (data:image/type;base64,data)
            if (!base64String.contains(";base64,")) {
                return false;
            }
            
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private Uni<Double> calculateAmount(Long planId, String tokensPackage) {
        if (planId != null) {
            // Buscar el plan y obtener su precio
            return subscriptionPlanRepository.findById(planId)
                    .map(plan -> plan.pricePen.doubleValue())
                    .onFailure().recoverWithItem(0.0);
        } else if (tokensPackage != null) {
            // Calcular precio basado en el paquete de tokens
            Double amount = switch (tokensPackage) {
                case "100" -> 15.0;
                case "500" -> 65.0;
                case "1000" -> 120.0;
                case "5000" -> 550.0;
                default -> 0.0;
            };
            return Uni.createFrom().item(amount);
        }
        return Uni.createFrom().item(0.0);
    }

    private String getStatusMessage(String status, boolean isExpired) {
        return switch (status) {
            case "pending" -> isExpired ? "Código de pago expirado" : "Pago pendiente de revisión";
            case "paid" -> "Pago aprobado y procesado";
            case "expired" -> "Código de pago expirado";
            default -> "Estado desconocido";
        };
    }

    private Uni<String> activatePlanOrTokens(Long adminId, PaymentCode code) {
        if (code.planId != null) {
            // Activar suscripción
            return activateSubscription(adminId, code.planId)
                    .map(result -> "Suscripción activada");
        } else if (code.tokensPackage != null) {
            // Agregar tokens
            int tokensToAdd = Integer.parseInt(code.tokensPackage);
            return tokenService.addTokens(adminId, tokensToAdd)
                    .map(result -> "Tokens agregados: " + tokensToAdd);
        }
        return Uni.createFrom().item("Activación completada");
    }

    private Uni<String> activateSubscription(Long adminId, Long planId) {
        // TODO: Implementar activación de suscripción
        log.info("🔄 Activando suscripción para admin " + adminId + ", plan " + planId);
        return Uni.createFrom().item("Suscripción activada");
    }

    @WithTransaction
    public Uni<List<PaymentHistoryResponse>> getPaymentHistory(Long adminId, String period) {
        log.info("📋 ManualPaymentService.getPaymentHistory() - AdminId: " + adminId + ", Period: " + period);
        
        return paymentHistoryRepository.findByAdminId(adminId)
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("⚠️ Error obteniendo historial de pagos para admin " + adminId + ": " + throwable.getMessage());
                    return List.<PaymentHistory>of(); // Retornar lista vacía en caso de error
                })
                .map(payments -> {
                    if (payments == null || payments.isEmpty()) {
                        log.info("📋 No hay historial de pagos para admin " + adminId);
                        return List.<PaymentHistoryResponse>of(); // Retornar lista vacía
                    }
                    
                    return payments.stream()
                            .map(payment -> new PaymentHistoryResponse(
                                    payment.id,
                                    payment.adminId,
                                    payment.paymentType,
                                    payment.amountPen.doubleValue(),
                                    "PEN",
                                    payment.status,
                                    payment.paymentMethod,
                                    payment.notes,
                                    payment.createdAt,
                                    payment.createdAt, // Usar createdAt como processedAt por ahora
                                    payment.transactionId
                            ))
                            .toList();
                });
    }
}
