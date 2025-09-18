package org.sky.dto.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record YapeNotificationResponse(
    @JsonProperty("notificationId")
    Long notificationId,
    
    @JsonProperty("transactionId")
    String transactionId,
    
    @JsonProperty("amount")
    Double amount,
    
    @JsonProperty("senderPhone")
    String senderPhone,
    
    @JsonProperty("senderName")
    String senderName,
    
    @JsonProperty("receiverPhone")
    String receiverPhone,
    
    @JsonProperty("status")
    String status,
    
    @JsonProperty("processedAt")
    LocalDateTime processedAt,
    
    @JsonProperty("message")
    String message
) {
}
