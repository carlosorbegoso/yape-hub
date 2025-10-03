package org.sky.dto.response.payment;

import java.time.LocalDateTime;

public record PaymentNotificationResponse(
    Long paymentId,
    Double amount,
    String senderName,
    String yapeCode,
    String status,
    LocalDateTime timestamp,
    String message
) {
    // Constructor compacto - validaciones y normalizaciones
    public PaymentNotificationResponse {
        // Validaciones
        if (paymentId == null) {
            throw new IllegalArgumentException("Payment ID cannot be null");
        }
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (senderName == null || senderName.trim().isEmpty()) {
            throw new IllegalArgumentException("Sender name cannot be null or empty");
        }
        if (yapeCode == null || yapeCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Yape code cannot be null or empty");
        }
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }
        if (timestamp == null) {
            timestamp = LocalDateTime.now(); // Default to current time
        }
        
        // Normalizaciones
        senderName = senderName.trim();
        yapeCode = yapeCode.trim();
        status = status.trim().toUpperCase();
        message = message != null ? message.trim() : "Pending payment confirmation";
        
        // Validaciones de dominio
        if (!isValidStatus(status)) {
            throw new IllegalArgumentException("Invalid payment status: " + status);
        }
    }
    
    // Constructor desde PaymentNotificationEntity
    public static PaymentNotificationResponse fromEntity(org.sky.model.PaymentNotificationEntity payment) {
        if (payment == null) {
            throw new IllegalArgumentException("Payment entity cannot be null");
        }
        
        return new PaymentNotificationResponse(
            payment.id,
            payment.amount.doubleValue(),
            payment.senderName,
            payment.yapeCode,
            payment.status,
            payment.createdAt,
            getStatusMessage(payment.status)
        );
    }
    
    // Constructor con mensaje personalizado
    public static PaymentNotificationResponse withMessage(Long paymentId, 
                                                        Double amount, 
                                                        String senderName, 
                                                        String yapeCode, 
                                                        String status, 
                                                        LocalDateTime timestamp, 
                                                        String customMessage) {
        return new PaymentNotificationResponse(
            paymentId, amount, senderName, yapeCode, status, timestamp, customMessage
        );
    }
    
    // Métodos auxiliares privados
    private static boolean isValidStatus(String status) {
        return status != null && 
               (status.equals("PENDING") || status.equals("CLAIMED") || status.equals("REJECTED"));
    }
    
    private static String getStatusMessage(String status) {
        return switch (status) {
            case "PENDING" -> "Pending payment confirmation";
            case "CLAIMED" -> "Payment claimed successfully";
            case "REJECTED" -> "Payment rejected";
            default -> "Payment status: " + status;
        };
    }
}
