package org.sky.interceptor;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.sky.dto.response.ErrorResponse;

import java.util.Map;

/**
 * Interceptor para manejar requests sin Authorization header
 * Solo intercepta endpoints que requieren autenticación
 * 
 * DESHABILITADO: Causaba problemas con endpoints públicos
 */
// @Provider  // Comentado para deshabilitar
@Priority(2000) // Ejecutar después de la autenticación JWT
public class AuthenticationInterceptor implements ContainerRequestFilter {
    
    private static final Logger log = Logger.getLogger(AuthenticationInterceptor.class);
    
    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        String method = requestContext.getMethod();
        
        // Skip para endpoints públicos
        if (isPublicEndpoint(path)) {
            return;
        }
        
        // Solo interceptar si no hay header de Authorization
        String authHeader = requestContext.getHeaderString("Authorization");
        
        if (authHeader == null || authHeader.trim().isEmpty()) {
            log.warn("🔒 Missing Authorization header for: " + method + " " + path);
            
            ErrorResponse errorResponse = ErrorResponse.withDetails(
                "Authentication required. Please provide a valid JWT token.",
                "UNAUTHORIZED",
                Map.of(
                    "error_type", "missing_authentication",
                    "hint", "Include 'Authorization: Bearer <your-jwt-token>' header",
                    "endpoint", path,
                    "method", method
                )
            );
            
            Response response = Response.status(Response.Status.UNAUTHORIZED)
                    .entity(errorResponse)
                    .build();
            
            requestContext.abortWith(response);
        }
        
        // Si hay header, dejar que Quarkus JWT maneje la validación
        // Los errores de JWT inválido serán manejados por los Exception Mappers
    }
    
    /**
     * Determina si un endpoint es público (no requiere autenticación)
     */
    private boolean isPublicEndpoint(String path) {
        // Normalizar path (remover / inicial si existe)
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        
        return normalizedPath.startsWith("api/auth/") ||
               normalizedPath.startsWith("q/") ||  // Quarkus endpoints (health, metrics, etc.)
               normalizedPath.startsWith("swagger-ui") ||
               normalizedPath.startsWith("openapi") ||
               normalizedPath.equals("api/auth/login") ||
               normalizedPath.equals("api/auth/admin/register") ||
               normalizedPath.equals("api/auth/refresh") ||
               normalizedPath.equals("api/auth/forgot-password") ||
               normalizedPath.equals("api/auth/seller/login-by-phone") ||
               normalizedPath.equals("") ||
               normalizedPath.equals("/");
    }
}