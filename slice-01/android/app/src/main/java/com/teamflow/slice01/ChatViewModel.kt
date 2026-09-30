package com.teamflow.slice01

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import java.util.UUID

class ChatViewModel : ViewModel() {

    private val repository = WebSocketChatRepository()

    private val _state = mutableStateOf(ChatUiState())
    val state: State<ChatUiState> = _state

    fun connect(userId: String) {
        if (userId.isBlank()) return

        _state.value = _state.value.copy(
            status = ChatStatus.CONNECTING,
            error = null
        )

        repository.connect(
            userId = userId,
            onConnected = {
                _state.value = _state.value.copy(
                    status = ChatStatus.CONNECTED,
                    error = null
                )
            },
            onMessage = { message ->
                _state.value = _state.value.copy(
                    messages = _state.value.messages + message,
                    error = null
                )
            },
            onError = { error ->
                _state.value = _state.value.copy(
                    status = ChatStatus.ERROR,
                    error = error
                )
            },
            onClosed = {
                _state.value = _state.value.copy(
                    status = ChatStatus.DISCONNECTED
                )
            }
        )
    }

    fun sendMessage(targetUserId: String, content: String) {
        if (targetUserId.isBlank() || content.isBlank()) return

        val messageId = UUID.randomUUID().toString()

        repository.sendMessage(
            messageId = messageId,
            targetUserId = targetUserId,
            content = content
        )

        // The first slice has no server persistence.
        // Add the sender's own copy locally for a simple chat experience.
        _state.value = _state.value.copy(
            messages = _state.value.messages + ChatMessageItem(
                fromUserId = "me",
                content = content
            )
        )
    }

    fun disconnect() {
        repository.disconnect()
        _state.value = _state.value.copy(
            status = ChatStatus.DISCONNECTED
        )
    }

    override fun onCleared() {
        repository.disconnect()
        super.onCleared()
    }
}

enum class ChatStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class ChatUiState(
    val status: ChatStatus = ChatStatus.DISCONNECTED,
    val messages: List<ChatMessageItem> = emptyList(),
    val error: String? = null
)

data class ChatMessageItem(
    val fromUserId: String,
    val content: String
)
