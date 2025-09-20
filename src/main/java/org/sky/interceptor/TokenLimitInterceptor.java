package org.sky.interceptor;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.sky.annotation.TokenConsumption;
import org.sky.service.TokenService;

import java.lang.reflect.Method;

@Interceptor
@TokenConsumption
public class TokenLimitInterceptor {

    @Inject
    TokenService tokenService;

    @AroundInvoke
    public Object checkTokenLimits(InvocationContext context) throws Exception {
        Method method = context.getMethod();
        
        // Verificar si el método requiere tokens
        TokenConsumption tokenConsumption = method.getAnnotation(TokenConsumption.class);
        if (tokenConsumption != null) {
            Log.info("🪙 TokenLimitInterceptor.checkTokenLimits() - Verificando tokens para: " + tokenConsumption.operationType());
            
            // Extraer adminId del contexto
            Long adminId = extractAdminIdFromContext(context);
            if (adminId != null) {
                // Verificar y consumir tokens
                return tokenService.consumeTokens(adminId, tokenConsumption.operationType(), tokenConsumption.tokens())
                        .chain(success -> {
                            if (success) {
                                try {
                                    return Uni.createFrom().item(context.proceed());
                                } catch (Exception e) {
                                    return Uni.createFrom().failure(e);
                                }
                            } else {
                                return Uni.createFrom().failure(new TokenService.InsufficientTokensException("Tokens insuficientes"));
                            }
                        });
            } else {
                Log.warn("❌ No se pudo extraer adminId del contexto");
                return context.proceed();
            }
        }
        
        return context.proceed();
    }

    private Long extractAdminIdFromContext(InvocationContext context) {
        try {
            // Intentar extraer adminId de los parámetros del método
            Object[] parameters = context.getParameters();
            for (Object param : parameters) {
                if (param instanceof Long) {
                    return (Long) param;
                }
            }
            
            // Si no se encuentra en parámetros, intentar extraer de query params
            // Esto requeriría acceso al contexto HTTP, que es más complejo
            Log.warn("⚠️ No se pudo extraer adminId automáticamente del contexto");
            return null;
            
        } catch (Exception e) {
            Log.error("❌ Error extrayendo adminId del contexto: " + e.getMessage());
            return null;
        }
    }
}
