package com.teamflow.slice01.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Registers raw WebSocket support.
 *
 * <p>No STOMP, Kafka, Redis, or message broker is involved in this slice.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final DevUserIdHandshakeInterceptor identityInterceptor;

    public WebSocketConfig(
            ChatWebSocketHandler chatWebSocketHandler,
            DevUserIdHandshakeInterceptor identityInterceptor
    ) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.identityInterceptor = identityInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws")
                .addInterceptors(identityInterceptor)
                // Local learning demo only. Tighten this for production.
                .setAllowedOriginPatterns("*");
    }
}
