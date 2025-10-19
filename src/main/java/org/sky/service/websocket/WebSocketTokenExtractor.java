package org.sky.service.websocket;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;

import java.util.List;
import java.util.Map;

import static io.smallrye.config._private.ConfigLogging.log;

@ApplicationScoped
public class WebSocketTokenExtractor {

  public Uni<String> extractTokenFromSession(Session session) {
    return Uni.createFrom().item(() -> {
      log.info("🔍 Attempting to extract token from WebSocket session");
      log.info("🔍 Session Query String: " + session.getQueryString());
      log.info("🔍 Session Request Parameters: " + session.getRequestParameterMap());

      try {
        // Existing token extraction logic
        String tokenFromQuery = extractTokenFromQuery(session);
        if (tokenFromQuery != null) {
          log.info("✅ Token extracted from query parameters");
          return tokenFromQuery;
        }

        String tokenFromHeaders = extractTokenFromHeaders(session);
        if (tokenFromHeaders != null) {
          log.info("✅ Token extracted from headers");
          return tokenFromHeaders;
        }

        log.warn("⚠️ No authentication token found in WebSocket session");
        return null;
      } catch (Exception e) {
        log.error("❌ Error extracting token from WebSocket session", e);
        return null;
      }
    });
  }

  private void logSessionDetails(Session session) {
    log.info("🔍 Session Query String: " + session.getQueryString());
    log.info("🔍 Session Request Parameters: " + session.getRequestParameterMap());
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
