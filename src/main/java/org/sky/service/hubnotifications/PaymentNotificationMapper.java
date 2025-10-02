package org.sky.service.hubnotifications;

import jakarta.enterprise.context.ApplicationScoped;
import org.sky.dto.payment.PaymentNotificationRequest;
import org.sky.dto.payment.PaymentNotificationResponse;
import org.sky.model.PaymentNotification;
import org.sky.model.SellerEntity;

import java.util.function.Function;


@ApplicationScoped
public class PaymentNotificationMapper {


    public static final Function<PaymentNotificationRequest, PaymentNotification> REQUEST_TO_ENTITY = request -> {
        PaymentNotification payment = new PaymentNotification();
        payment.adminId = request.adminId();
        payment.amount = request.amount();
        payment.senderName = request.senderName();
        payment.yapeCode = request.yapeCode();
        payment.deduplicationHash = generateDeduplicationHash(request);
        payment.status = "PENDING";
        return payment;
    };


    public static final Function<PaymentNotification, PaymentNotificationResponse> ENTITY_TO_RESPONSE = payment -> 
        new PaymentNotificationResponse(
            payment.id,
            payment.amount,
            payment.senderName,
            payment.yapeCode,
            payment.status,
            payment.createdAt,
            "Pending payment confirmation"
        );


    public static final Function<PaymentNotificationRequest, Function<SellerEntity, PaymentNotification>> REQUEST_WITH_SELLER_TO_ENTITY = 
        request -> seller -> {
            PaymentNotification payment = REQUEST_TO_ENTITY.apply(request);
            return payment;
        };


    private static String generateDeduplicationHash(PaymentNotificationRequest request) {
        try {
            return java.security.MessageDigest.getInstance("MD5")
                .digest((request.adminId() + ":" + request.amount() + ":" + request.yapeCode() + ":" + System.currentTimeMillis()).getBytes())
                .toString();
        } catch (Exception e) {
            return request.adminId() + ":" + request.amount() + ":" + request.yapeCode() + ":" + System.currentTimeMillis();
        }
    }


    public static final Function<java.util.List<PaymentNotificationResponse>, String> TO_GROUPED_JSON = notifications -> {
        double totalAmount = notifications.stream().mapToDouble(PaymentNotificationResponse::amount).sum();
        int count = notifications.size();
        
        return String.format(
            "{\"type\":\"GROUPED_PAYMENT_NOTIFICATION\",\"data\":{\"count\":%d,\"totalAmount\":%.2f,\"payments\":[%s],\"message\":\"%d new payments received - Total: S/ %.2f\"}}",
            count,
            totalAmount,
            notifications.stream()
                .map(n -> String.format("{\"paymentId\":%d,\"amount\":%.2f,\"senderName\":\"%s\",\"yapeCode\":\"%s\"}", 
                    n.paymentId(), n.amount(), n.senderName(), n.yapeCode()))
                .collect(java.util.stream.Collectors.joining(",")),
            count,
            totalAmount
        );
    };


    public static final Function<PaymentNotificationResponse, String> TO_INDIVIDUAL_JSON = notification ->
        String.format(
            "{\"type\":\"PAYMENT_NOTIFICATION\",\"data\":{\"paymentId\":%d,\"amount\":%.2f,\"senderName\":\"%s\",\"yapeCode\":\"%s\",\"message\":\"%s\"}}",
            notification.paymentId(),
            notification.amount(),
            notification.senderName(),
            notification.yapeCode(),
            notification.message()
        );
}
