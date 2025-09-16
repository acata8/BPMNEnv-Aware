package org.unicam.intermediate.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.unicam.intermediate.websocket.GpsWebSocketHandler;
import org.unicam.intermediate.websocket.GpsHandshakeInterceptor;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketConfigurer {

    private final GpsWebSocketHandler gpsWebSocketHandler;
    private final GpsHandshakeInterceptor gpsHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Register with SockJS support
        registry.addHandler(gpsWebSocketHandler, "/ws/gps")
                .addInterceptors(gpsHandshakeInterceptor)
                .setAllowedOriginPatterns("*")  // Allow all origins for development
                .withSockJS();  // This enables SockJS fallback at /ws/gps/info

        // Also register raw WebSocket endpoint (without SockJS)
        registry.addHandler(gpsWebSocketHandler, "/ws/gps/raw")
                .addInterceptors(gpsHandshakeInterceptor)
                .setAllowedOriginPatterns("*");

        log.info("[WebSocketConfig] Registered GPS WebSocket handlers");
    }
}