package org.sky.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "token_usage_log")
public class TokenUsageLog extends PanacheEntity {

    @Column(name = "admin_id", nullable = false)
    public Long adminId;

    @Column(name = "operation_type", nullable = false, length = 50)
    public String operationType; // payment, qr_generation, websocket, analytics, api_call

    @Column(name = "tokens_consumed", nullable = false)
    public Integer tokensConsumed;

    @Column(name = "operation_details", columnDefinition = "JSONB")
    public String operationDetails; // JSON con detalles de la operación

    @CreationTimestamp
    @Column(name = "created_at")
    public LocalDateTime createdAt;

    // Constructores
    public TokenUsageLog() {}

    public TokenUsageLog(Long adminId, String operationType, Integer tokensConsumed, String operationDetails) {
        this.adminId = adminId;
        this.operationType = operationType;
        this.tokensConsumed = tokensConsumed;
        this.operationDetails = operationDetails;
    }

    // Métodos de utilidad
    public static TokenUsageLog createPaymentLog(Long adminId, Integer tokensConsumed, String paymentId) {
        String details = String.format("{\"paymentId\": \"%s\", \"timestamp\": %d}", 
                                     paymentId, System.currentTimeMillis());
        return new TokenUsageLog(adminId, "payment", tokensConsumed, details);
    }

    public static TokenUsageLog createQrLog(Long adminId, Integer tokensConsumed, String qrCode) {
        String details = String.format("{\"qrCode\": \"%s\", \"timestamp\": %d}", 
                                     qrCode, System.currentTimeMillis());
        return new TokenUsageLog(adminId, "qr_generation", tokensConsumed, details);
    }

    public static TokenUsageLog createWebSocketLog(Long adminId, Integer tokensConsumed, String sellerId) {
        String details = String.format("{\"sellerId\": \"%s\", \"timestamp\": %d}", 
                                     sellerId, System.currentTimeMillis());
        return new TokenUsageLog(adminId, "websocket", tokensConsumed, details);
    }

    public static TokenUsageLog createAnalyticsLog(Long adminId, Integer tokensConsumed, String reportType) {
        String details = String.format("{\"reportType\": \"%s\", \"timestamp\": %d}", 
                                     reportType, System.currentTimeMillis());
        return new TokenUsageLog(adminId, "analytics", tokensConsumed, details);
    }
}
