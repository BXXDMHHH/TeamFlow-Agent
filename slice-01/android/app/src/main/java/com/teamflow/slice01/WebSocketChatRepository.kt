package com.teamflow.slice01

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

/**
 * Small wrapper around OkHttp's raw WebSocket API.
 *
 * <p>The standalone demo uses ?userId=... only because the real JWT
 * authentication implementation is not stored in this repository.
 *
 * <p>Replace this development URL identity with an Authorization header when
 * integrating into the main TeamFlow application.
 */
class WebSocketChatRepository {

    private val client = OkHttpClient()

    private var webSocket: WebSocket? = null

    fun connect(
        userId: String,
        onConnected: () -> Unit,
        onMessage: (ChatMessageItem) -> Unit,
        onError: (String) -> Unit,
        onClosed: () -> Unit
    ) {
        // Android Emulator reaches the host Windows machine through 10.0.2.2.
        val url = "ws://10.0.2.2:8080/ws?userId=\${userId.encodeForUrl()}"

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(
            request,
            object : WebSocketListener() {

                override fun onOpen(webSocket: WebSocket, response: Response) {
                    onConnected()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val root = JSONObject(text)

                        if (root.optString("type") != "MESSAGE_RECEIVED") {
                            return
                        }

                        val payload = root.getJSONObject("payload")

                        onMessage(
                            ChatMessageItem(
                                fromUserId = payload.getString("fromUserId"),
                                content = payload.getString("content")
                            )
                        )
                    } catch (exception: Exception) {
                        onError("Invalid server message: \${exception.message}")
                    }
                }

                override fun onFailure(
                    webSocket: WebSocket,
                    t: Throwable,
                    response: Response?
                ) {
                    onError(t.message ?: "WebSocket failure")
                }

                override fun onClosed(
                    webSocket: WebSocket,
                    code: Int,
                    reason: String
                ) {
                    onClosed()
                }
            }
        )
    }

    fun sendMessage(
        messageId: String,
        targetUserId: String,
        content: String
    ) {
        val root = JSONObject()
            .put("type", "SEND_MESSAGE")
            .put("messageId", messageId)
            .put("timestamp", System.currentTimeMillis())
            .put(
                "payload",
                JSONObject()
                    .put("toUserId", targetUserId)
                    .put("content", content)
            )

        webSocket?.send(root.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "Client closed")
        webSocket = null
    }

    private fun String.encodeForUrl(): String {
        return java.net.URLEncoder.encode(this, Charsets.UTF_8.name())
    }
}
