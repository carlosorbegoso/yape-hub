package org.sky.service.payment;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PaymentImageValidator {

    public Uni<Boolean> validateImage(String base64String) {
        return isValidBase64(base64String)
                .chain(isValid -> {
                    if (Boolean.FALSE.equals(isValid)) {
                        return Uni.createFrom().failure(new RuntimeException("Invalid base64 image format"));
                    }
                    return Uni.createFrom().item(true);
                });
    }

    private Uni<Boolean> isValidBase64(String base64String) {
        return Uni.createFrom().item(() -> {
            if (base64String == null || base64String.trim().isEmpty()) {
                return false;
            }
            
            try {
                java.util.Base64.getDecoder().decode(base64String);
                
                if (!base64String.startsWith("data:image/")) {
                    return false;
                }

                return base64String.contains(";base64,");
            } catch (IllegalArgumentException e) {
                return false;
            }
        });
    }
}
