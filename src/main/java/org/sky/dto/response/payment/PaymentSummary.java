package org.sky.dto.response.payment;

public record PaymentSummary(
    Long totalPayments,
    Long pendingCount,
    Long confirmedCount,
    Long rejectedCount,
    Double totalAmount,
    Double confirmedAmount,
    Double pendingAmount
) {
    // Constructor compacto - validaciones y normalizaciones
    public PaymentSummary {
        // Validaciones
        if (totalPayments == null || totalPayments < 0) {
            throw new IllegalArgumentException("Total payments cannot be negative");
        }
        if (pendingCount == null || pendingCount < 0) {
            throw new IllegalArgumentException("Pending count cannot be negative");
        }
        if (confirmedCount == null || confirmedCount < 0) {
            throw new IllegalArgumentException("Confirmed count cannot be negative");
        }
        if (rejectedCount == null || rejectedCount < 0) {
            throw new IllegalArgumentException("Rejected count cannot be negative");
        }
        if (totalAmount == null || totalAmount < 0) {
            throw new IllegalArgumentException("Total amount cannot be negative");
        }
        if (confirmedAmount == null || confirmedAmount < 0) {
            throw new IllegalArgumentException("Confirmed amount cannot be negative");
        }
        if (pendingAmount == null || pendingAmount < 0) {
            throw new IllegalArgumentException("Pending amount cannot be negative");
        }
        
        // Validaciones de consistencia
        if (totalPayments != (pendingCount + confirmedCount + rejectedCount)) {
            throw new IllegalArgumentException("Total payments must equal sum of pending, confirmed, and rejected counts");
        }
        if (totalAmount < (confirmedAmount + pendingAmount)) {
            throw new IllegalArgumentException("Total amount must be greater than or equal to sum of confirmed and pending amounts");
        }
        
        // Valores por defecto
        if (totalPayments == null) totalPayments = 0L;
        if (pendingCount == null) pendingCount = 0L;
        if (confirmedCount == null) confirmedCount = 0L;
        if (rejectedCount == null) rejectedCount = 0L;
        if (totalAmount == null) totalAmount = 0.0;
        if (confirmedAmount == null) confirmedAmount = 0.0;
        if (pendingAmount == null) pendingAmount = 0.0;
    }
    
    // Constructor de conveniencia
    public static PaymentSummary create(Long totalPayments, Long pendingCount, Long confirmedCount, 
                                      Long rejectedCount, Double totalAmount, Double confirmedAmount, 
                                      Double pendingAmount) {
        return new PaymentSummary(totalPayments, pendingCount, confirmedCount, rejectedCount, 
                                totalAmount, confirmedAmount, pendingAmount);
    }
    
    // Constructor vacío
    public static PaymentSummary empty() {
        return new PaymentSummary(0L, 0L, 0L, 0L, 0.0, 0.0, 0.0);
    }
    
    // Métodos de conveniencia
    public boolean isEmpty() {
        return totalPayments == 0;
    }
    
    public double rejectedAmount() {
        return totalAmount - confirmedAmount - pendingAmount;
    }
    
    public double averageAmount() {
        return totalPayments > 0 ? totalAmount / totalPayments : 0.0;
    }
    
    public double confirmationRate() {
        return totalPayments > 0 ? (double) confirmedCount / totalPayments : 0.0;
    }
    
    public double rejectionRate() {
        return totalPayments > 0 ? (double) rejectedCount / totalPayments : 0.0;
    }
}
