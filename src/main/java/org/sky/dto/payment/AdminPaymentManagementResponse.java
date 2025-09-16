package org.sky.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public record AdminPaymentManagementResponse(
    @JsonProperty("payments")
    List<PaymentDetail> payments,
    
    @JsonProperty("summary")
    PaymentSummary summary,
    
    @JsonProperty("pagination")
    PaginationInfo pagination
) {
    public record PaymentDetail(
        @JsonProperty("paymentId")
        Long paymentId,
        
        @JsonProperty("amount")
        Double amount,
        
        @JsonProperty("senderName")
        String senderName,
        
        @JsonProperty("yapeCode")
        String yapeCode,
        
        @JsonProperty("status")
        String status,
        
        @JsonProperty("createdAt")
        LocalDateTime createdAt,
        
        @JsonProperty("confirmedBy")
        Long confirmedBy,
        
        @JsonProperty("confirmedAt")
        LocalDateTime confirmedAt,
        
        @JsonProperty("rejectedBy")
        Long rejectedBy,
        
        @JsonProperty("rejectedAt")
        LocalDateTime rejectedAt,
        
        @JsonProperty("rejectionReason")
        String rejectionReason,
        
        @JsonProperty("sellerName")
        String sellerName,
        
        @JsonProperty("branchName")
        String branchName
    ) {}
    
    public record PaymentSummary(
        @JsonProperty("totalPayments")
        Long totalPayments,
        
        @JsonProperty("pendingCount")
        Long pendingCount,
        
        @JsonProperty("confirmedCount")
        Long confirmedCount,
        
        @JsonProperty("rejectedCount")
        Long rejectedCount,
        
        @JsonProperty("totalAmount")
        Double totalAmount,
        
        @JsonProperty("confirmedAmount")
        Double confirmedAmount,
        
        @JsonProperty("pendingAmount")
        Double pendingAmount
    ) {}
    
    public record PaginationInfo(
        @JsonProperty("currentPage")
        int currentPage,
        
        @JsonProperty("totalPages")
        int totalPages,
        
        @JsonProperty("totalItems")
        long totalItems,
        
        @JsonProperty("itemsPerPage")
        int itemsPerPage
    ) {}
}
