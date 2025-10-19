package org.sky.service.security;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class SecurityService {

    @Inject
    AuthorizationService authorizationService;
    
    @Inject
    SecurityErrorHandler securityErrorHandler;

    public Uni<Long> validateJwtToken(String authorization) {
        return authorizationService.validateAdminAuthorization(authorization, null);
    }

    public Uni<Long> validateAdminAuthorization(String authorization, Long adminId) {
        return authorizationService.validateAdminAuthorization(authorization, adminId);
    }

    public Uni<Long> validateSellerAuthorization(String authorization, Long sellerId) {
        return authorizationService.validateSellerAuthorization(authorization, sellerId);
    }

  public Response handleSecurityException(Throwable throwable) {
        return securityErrorHandler.handleSecurityException(throwable);
    }
}
