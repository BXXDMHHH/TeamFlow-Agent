package com.teamflow.slice01.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Minimal A -> B message forwarder.
 *
 * <p>Flow:
 *
 * <pre>
 * A sends JSON
 *   -> read A's identity
 *   -> read toUserId
 *   -> find B's WebSocketSession
 *   -> send MESSAGE_RECEIVED to B
 * </pre>
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ConnectionManager connectionManager;

    public ChatWebSocketHandler(
            ObjectMapper objectMapper,
            ConnectionManager connectionManager
    ) {
        this.objectMapper = objectMapper;
        this.connectionManager = connectionManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = currentUserId(session);

        if (userId == null || userId.isBlank()) {
            closeQuietly(
                    session,
                    CloseStatus.NOT_ACCEPTABLE.withReason("Missing user identity")
            );
            return;
        }

        connectionManager.register(userId, session);

        System.out.printf(
                "WebSocket connected: userId=%s, sessionId=%s%n",
                userId,
                session.getId()
        );
    }

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message
    ) {
        String senderId = currentUserId(session);

        if (senderId == null || senderId.isBlank()) {
            closeQuietly(
                    session,
                    CloseStatus.NOT_ACCEPTABLE.withReason("Missing user identity")
            );
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(message.getPayload());

            String type = text(root, "type");
            String messageId = text(root, "messageId");
            String toUserId = root.path("payload").path("toUserId").asText(null);
            String content = root.path("payload").path("content").asText(null);

            if (!"SEND_MESSAGE".equals(type)) {
                sendRejected(session, messageId, "UNSUPPORTED_MESSAGE_TYPE");
                return;
            }

            if (messageId == null || messageId.isBlank()) {
                sendRejected(session, null, "MESSAGE_ID_REQUIRED");
                return;
            }

            if (toUserId == null || toUserId.isBlank()) {
                sendRejected(session, messageId, "TO_USER_ID_REQUIRED");
                return;
            }

            if (content == null || content.isBlank()) {
                sendRejected(session, messageId, "CONTENT_REQUIRED");
                return;
            }

            WebSocketSession targetSession = connectionManager.find(toUserId);

            if (targetSession == null || !targetSession.isOpen()) {
                sendRejected(session, messageId, "RECIPIENT_OFFLINE");
                return;
            }

            ObjectNode outbound = objectMapper.createObjectNode();
            outbound.put("type", "MESSAGE_RECEIVED");
            outbound.put("messageId", messageId);
            outbound.put("timestamp", System.currentTimeMillis());

            ObjectNode payload = outbound.putObject("payload");
            payload.put("fromUserId", senderId);
            payload.put("content", content);

            targetSession.sendMessage(
                    new TextMessage(objectMapper.writeValueAsString(outbound))
            );

            System.out.printf(
                    "Message forwarded: from=%s, to=%s, messageId=%s%n",
                    senderId,
                    toUserId,
                    messageId
            );
        } catch (Exception exception) {
            System.err.println(
                    "WebSocket message handling failed: " + exception.getMessage()
            );
            sendRejected(session, null, "INVALID_MESSAGE");
        }
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status
    ) {
        String userId = currentUserId(session);

        if (userId != null) {
            connectionManager.remove(userId, session);
        }

        System.out.printf(
                "WebSocket disconnected: userId=%s, sessionId=%s, status=%s%n",
                userId,
                session.getId(),
                status
        );
    }

    @Override
    public void handleTransportError(
            WebSocketSession session,
            Throwable exception
    ) {
        System.err.printf(
                "WebSocket transport error: sessionId=%s, error=%s%n",
                session.getId(),
                exception.getMessage()
        );
    }

    private String currentUserId(WebSocketSession session) {
        if (session.getPrincipal() != null
                && session.getPrincipal().getName() != null) {
            return session.getPrincipal().getName();
        }

        Object devUserId = session.getAttributes().get("userId");
        return devUserId == null ? null : devUserId.toString();
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }

    private void sendRejected(
            WebSocketSession session,
            String messageId,
            String reason
    ) {
        if (!session.isOpen()) {
            return;
        }

        try {
            ObjectNode rejected = objectMapper.createObjectNode();
            rejected.put("type", "MESSAGE_REJECTED");

            if (messageId == null) {
                rejected.putNull("messageId");
            } else {
                rejected.put("messageId", messageId);
            }

            rejected.put("timestamp", System.currentTimeMillis());

            ObjectNode payload = rejected.putObject("payload");
            payload.put("reason", reason);

            session.sendMessage(
                    new TextMessage(objectMapper.writeValueAsString(rejected))
            );
        } catch (Exception ignored) {
            // The client may have disconnected already.
        }
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (Exception ignored) {
            // Ignore close failures during teardown.
        }
    }
}
