package org.unicam.intermediate.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@Slf4j
public class GpsHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String userId = servletRequest.getServletRequest().getParameter("userId");
            String token = servletRequest.getServletRequest().getParameter("token");
            
            if (userId == null || userId.isBlank()) {
                log.warn("[GPS WS] Connection rejected - missing userId");
                return false;
            }
            
            // TODO: Bisogna ovviamente valutare il token o l'autorizzazione qui
            
            attributes.put("userId", userId);
            attributes.put("connectionTime", System.currentTimeMillis());
            
            log.info("[GPS WS] Handshake accepted for userId: {}", userId);
            return true;
        }
        
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Log any post-handshake issues
        if (exception != null) {
            log.error("[GPS WS] Handshake error", exception);
        }
    }
}