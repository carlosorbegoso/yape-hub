package org.sky.service.websocket;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class WebSocketTokenExtractor {

    public Uni<String> extractTokenFromSession(Session session) {
        return Uni.createFrom().item(() -> {
            try {
                String tokenFromQuery = extractTokenFromQuery(session);
                if (tokenFromQuery != null) {
                    return tokenFromQuery;
                }
                
                return extractTokenFromHeaders(session);
            } catch (Exception e) {
                return null;
            }
        });
    }

    private String extractTokenFromQuery(Session session) {
        String queryString = session.getQueryString();
        if (queryString == null || queryString.isEmpty()) {
            return null;
        }
        
        String[] params = queryString.split("&");
        for (String param : params) {
            if (param.startsWith("token=")) {
                return param.substring(6);
            }
        }
        return null;
    }

    private String extractTokenFromHeaders(Session session) {
        Map<String, List<String>> headers = session.getRequestParameterMap();
        if (headers == null || !headers.containsKey("authorization")) {
            return null;
        }
        
        List<String> authHeaders = headers.get("authorization");
        if (authHeaders == null || authHeaders.isEmpty()) {
            return null;
        }
        
        String authHeader = authHeaders.getFirst();
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        return null;
    }
}
