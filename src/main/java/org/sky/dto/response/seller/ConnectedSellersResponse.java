package org.sky.dto.response.seller;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para respuesta de vendedores conectados
 */
public record ConnectedSellersResponse(
    Long adminId,
    List<ConnectedSellerInfo> connectedSellers,
    Integer totalConnected,
    LocalDateTime timestamp
) {
    
    /**
     * Constructor principal
     */
    public static ConnectedSellersResponse create(Long adminId, List<ConnectedSellerInfo> sellers) {
        int totalConnected = (int) sellers.stream().filter(ConnectedSellerInfo::isConnected).count();
        return new ConnectedSellersResponse(adminId, sellers, totalConnected, LocalDateTime.now());
    }
}
