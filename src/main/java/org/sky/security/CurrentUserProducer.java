package org.sky.security;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;

@RequestScoped
public class CurrentUserProducer {

    @Inject
    JsonWebToken jwt;

    @Produces
    @RequestScoped
    @CurrentUser
    public Long getCurrentUserId() {
        if (jwt != null && jwt.getSubject() != null) {
            return Long.parseLong(jwt.getSubject());
        }
        throw new SecurityException("No authenticated user found");
    }
}
