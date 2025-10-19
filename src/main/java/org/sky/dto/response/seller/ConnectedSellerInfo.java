package org.sky.dto.response.seller;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.LocalDateTime;

/**
 * DTO para información de vendedor conectado
 */
@RegisterForReflection
public record ConnectedSellerInfo(
    Long sellerId,
    String sellerName,
    String email,
    String phone,
    Long branchId,
    String branchName,
    Boolean isConnected,
    LocalDateTime lastSeen
) {
    
    /**
     * Constructor para vendedor conectado
     */
    public static ConnectedSellerInfo connected(Long sellerId, String sellerName, String email, 
                                               String phone, Long branchId, String branchName, 
                                               LocalDateTime lastSeen) {
        return new ConnectedSellerInfo(sellerId, sellerName, email, phone, branchId, branchName, true, lastSeen);
    }
    
    /**
     * Constructor para vendedor desconectado
     */
    public static ConnectedSellerInfo disconnected(Long sellerId, String sellerName, String email, 
                                                 String phone, Long branchId, String branchName, 
                                                 LocalDateTime lastSeen) {
        return new ConnectedSellerInfo(sellerId, sellerName, email, phone, branchId, branchName, false, lastSeen);
    }
}
