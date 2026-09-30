package com.teamflow.slice01.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Very small in-memory connection registry.
 *
 * <p>One user is mapped to one active session in this first slice.
 * Do not replace this with Redis yet; that is a later learning step.
 */
@Component
public class ConnectionManager {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void register(String userId, WebSocketSession session) {
        sessions.put(userId, session);
    }

    /**
     * Remove the session only when it is still the current session.
     * This avoids an old connection deleting a newer connection.
     */
    public void remove(String userId, WebSocketSession session) {
        sessions.computeIfPresent(
                userId,
                (key, current) -> current.getId().equals(session.getId()) ? null : current
        );
    }

    public WebSocketSession find(String userId) {
        return sessions.get(userId);
    }

    public int size() {
        return sessions.size();
    }
}
