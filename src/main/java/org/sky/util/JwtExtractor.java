package org.sky.util;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.sky.exception.ValidationException;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class JwtExtractor {
    
    public Long extractUserIdFromToken(String token) {
        try {
            if (token == null) {
                throw new IllegalArgumentException("Token cannot be null");
            }
            
            // Remove "Bearer " prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            // Parse our simple JWT manually
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT format - expected 3 parts, got " + parts.length);
            }
            
            // Decode the payload (second part)
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            System.out.println("JWT Payload: " + payload); // Debug log
            
            // Extract subject using regex (works with both JSON and Map formats)
            Pattern pattern = Pattern.compile("sub=?(\\d+)");
            Matcher matcher = pattern.matcher(payload);
            
            if (matcher.find()) {
                String subject = matcher.group(1);
                System.out.println("Found subject: " + subject); // Debug log
                return Long.parseLong(subject);
            } else {
                throw new IllegalArgumentException("Subject not found in token payload: " + payload);
            }
            
        } catch (Exception e) {
            System.out.println("JWT extraction error: " + e.getMessage()); // Debug log
            throw ValidationException.invalidField("token", token, "Invalid or expired token");
        }
    }
    
    public Long extractSellerIdFromToken(String token) {
        try {
            // Remove "Bearer " prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            // Parse our simple JWT manually
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null; // Token inválido
            }
            
            // Decode the payload (second part)
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            
            // Extract sellerId using regex
            Pattern pattern = Pattern.compile("sellerId=?(\\d+)");
            Matcher matcher = pattern.matcher(payload);
            
            if (matcher.find()) {
                String sellerId = matcher.group(1);
                return Long.parseLong(sellerId);
            } else {
                return null; // sellerId no encontrado
            }
            
        } catch (Exception e) {
            return null; // Error al procesar token
        }
    }
    
    public Long extractUserIdFromSecurityContext(SecurityContext securityContext) {
        if (securityContext.getUserPrincipal() instanceof JsonWebToken jwt) {
            String subject = jwt.getSubject();
            return Long.parseLong(subject);
        }
        throw ValidationException.invalidField("authentication", "none", "User not authenticated");
    }
}
