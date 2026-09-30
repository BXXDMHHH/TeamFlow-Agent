package com.teamflow.slice01.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * Development-only identity bridge.
 *
 * <p>The main TeamFlow project already has JWT authentication, but the GitHub
 * repository did not contain that local authentication implementation when
 * this standalone slice was added.
 *
 * <p>For this demo the client connects with:
 *
 * <pre>
 * ws://10.0.2.2:8080/ws?userId=alice
 * </pre>
 *
 * <p>When this slice is merged into the real backend, replace this interceptor
 * with the existing JWT handshake authentication.
 */
@Component
public class DevUserIdHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        String userId = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("userId");

        if (userId == null || userId.isBlank()) {
            return false;
        }

        // TODO: Replace query-param identity with the JWT authenticated Principal.
        attributes.put("userId", userId);
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // Nothing to clean up here.
    }
}
