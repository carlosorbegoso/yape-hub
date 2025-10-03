package org.sky.service.hubnotifications;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.dto.request.payment.PaymentNotificationRequest;
import org.sky.exception.ValidationException;
import org.sky.model.SellerEntity;

import java.util.function.Function;
import java.util.function.LongFunction;

@ApplicationScoped
public class PaymentNotificationValidator {

    public static Function<PaymentNotificationRequest, Uni<PaymentNotificationRequest>> validateRequest() {
        return request -> Uni.createFrom().item(() -> {
            if (request == null) {
                throw ValidationException.requiredField("Payment request");
            }
            
            if (request.amount() <= 0) {
                throw ValidationException.invalidField("amount", request.amount().toString(), "must be greater than 0");
            }
            
            if (request.senderName() == null || request.senderName().trim().isEmpty()) {
                throw ValidationException.requiredField("senderName");
            }
            
            if (request.yapeCode() == null || request.yapeCode().trim().isEmpty()) {
                throw ValidationException.requiredField("yapeCode");
            }
            
            return request;
        });
    }

    public static Function<SellerEntity, Uni<SellerEntity>> validateSeller() {
        return seller -> Uni.createFrom().item(() -> {
            if (seller == null) {
                throw ValidationException.requiredField("seller");
            }
            
            if (!seller.isActive) {
                throw ValidationException.invalidField("seller", seller.id.toString(), "is not active");
            }
            
            return seller;
        });
    }

    public static LongFunction<Uni<Long>> validateAdminId() {
        return adminId -> Uni.createFrom().item(() -> {
            if (adminId <= 0) {
                throw ValidationException.invalidField("adminId", String.valueOf(adminId), "must be greater than 0");
            }
            return adminId;
        });
    }

    public static LongFunction<Uni<Long>> validateSellerId() {
        return sellerId -> Uni.createFrom().item(() -> {
            if (sellerId <= 0) {
                throw ValidationException.invalidField("sellerId", String.valueOf(sellerId), "must be greater than 0");
            }
            return sellerId;
        });
    }

    public static LongFunction<Uni<Long>> validatePaymentId() {
        return paymentId -> Uni.createFrom().item(() -> {
            if (paymentId <= 0) {
                throw ValidationException.invalidField("paymentId", String.valueOf(paymentId), "must be greater than 0");
            }
            return paymentId;
        });
    }

    public static Function<java.util.Map<String, Object>, Uni<java.util.Map<String, Object>>> validatePagination() {
        return params -> Uni.createFrom().item(() -> {
            int page = (Integer) params.getOrDefault("page", 0);
            int size = (Integer) params.getOrDefault("size", 10);
            
            if (page < 0) {
                throw ValidationException.invalidField("page", String.valueOf(page), "must be non-negative");
            }
            
            if (size <= 0 || size > 100) {
                throw ValidationException.invalidField("size", String.valueOf(size), "must be between 1 and 100");
            }
            
            params.put("page", page);
            params.put("size", size);
            return params;
        });
    }

    public static Function<java.util.Map<String, Object>, Uni<java.util.Map<String, Object>>> validateDateRange() {
        return params -> Uni.createFrom().item(() -> {
            java.time.LocalDate startDate = (java.time.LocalDate) params.get("startDate");
            java.time.LocalDate endDate = (java.time.LocalDate) params.get("endDate");
            
            if (startDate == null) {
                startDate = java.time.LocalDate.now().minusDays(30);
            }
            
            if (endDate == null) {
                endDate = java.time.LocalDate.now();
            }
            
            if (startDate.isAfter(endDate)) {
                throw ValidationException.invalidField("dateRange", startDate + " to " + endDate, "start date cannot be after end date");
            }
            
            params.put("startDate", startDate);
            params.put("endDate", endDate);
            return params;
        });
    }
}
