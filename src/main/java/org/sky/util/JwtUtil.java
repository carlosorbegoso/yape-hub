package org.sky.util;

import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.User;

import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class JwtUtil {
    
    private static final String SECRET_KEY = "yapechamo-secret-key-2024-very-long-secret-key-for-jwt-signing";
    private static final Duration ACCESS_TOKEN_DURATION = Duration.ofHours(1);
    private static final Duration REFRESH_TOKEN_DURATION = Duration.ofDays(7);
    
    public String generateAccessToken(Long userId, User.UserRole role) {
        return generateAccessToken(userId, role, null);
    }
    
    public String generateAccessToken(Long userId, User.UserRole role, Long sellerId) {
        try {
            // Create a simple JWT-like token for now
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", userId.toString());
            claims.put("iss", "http://localhost:8080");
            claims.put("groups", role.name());
            claims.put("exp", System.currentTimeMillis() / 1000 + ACCESS_TOKEN_DURATION.getSeconds());
            claims.put("iat", System.currentTimeMillis() / 1000);
            
            // Agregar sellerId si es un seller
            if (role == User.UserRole.SELLER && sellerId != null) {
                claims.put("sellerId", sellerId.toString());
            }
            
            // Create a simple base64 encoded token
            String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes());
            String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(claims.toString().getBytes());
            String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(SECRET_KEY.getBytes());
            
            return header + "." + payload + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate access token", e);
        }
    }
    
    public String generateRefreshToken(Long userId) {
        try {
            // Create a minimal refresh token with only essential information
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", userId.toString());
            claims.put("type", "refresh");
            claims.put("exp", System.currentTimeMillis() / 1000 + REFRESH_TOKEN_DURATION.getSeconds());
            
            // Create a simple base64 encoded token
            String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes());
            String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(claims.toString().getBytes());
            String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(SECRET_KEY.getBytes());
            
            return header + "." + payload + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate refresh token", e);
        }
    }
    
    public Long validateRefreshToken(String token) {
        try {
            // Parse our simple JWT manually
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            
            // Decode the payload (second part)
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            
            // Extract subject using regex (works with both JSON and Map formats)
            Pattern pattern = Pattern.compile("sub=?(\\d+)");
            Matcher matcher = pattern.matcher(payload);
            
            if (matcher.find()) {
                return Long.parseLong(matcher.group(1));
            } else {
                return null;
            }
            
        } catch (Exception e) {
            return null;
        }
    }
    
    public Long getUserIdFromToken(String token) {
        try {
            // Parse our simple JWT manually
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            
            // Decode the payload (second part)
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            
            // Extract subject using regex (works with both JSON and Map formats)
            Pattern pattern = Pattern.compile("sub=?(\\d+)");
            Matcher matcher = pattern.matcher(payload);
            
            if (matcher.find()) {
                return Long.parseLong(matcher.group(1));
            } else {
                return null;
            }
            
        } catch (Exception e) {
            return null;
        }
    }
    
    public Long getSellerIdFromToken(String token) {
        try {
            // Parse our simple JWT manually
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            
            // Decode the payload (second part)
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            
            // Extract sellerId using regex (supports both formats: sellerId=123 and "sellerId": 123)
            Pattern pattern = Pattern.compile("\"sellerId\"\\s*:\\s*(\\d+)|sellerId=?(\\d+)");
            Matcher matcher = pattern.matcher(payload);
            
            if (matcher.find()) {
                // Try group 1 first (JSON format: "sellerId": 123)
                String sellerIdStr = matcher.group(1);
                if (sellerIdStr != null) {
                    return Long.parseLong(sellerIdStr);
                }
                // Try group 2 (key=value format: sellerId=123)
                sellerIdStr = matcher.group(2);
                if (sellerIdStr != null) {
                    return Long.parseLong(sellerIdStr);
                }
            }
            return null;
            
        } catch (Exception e) {
            return null;
        }
    }
}
