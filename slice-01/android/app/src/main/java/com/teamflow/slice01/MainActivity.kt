package com.teamflow.slice01

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ChatScreen()
                }
            }
        }
    }
}

@Composable
private fun ChatScreen(
    chatViewModel: ChatViewModel = viewModel()
) {
    val state by chatViewModel.state

    var userId by remember { mutableStateOf("alice") }
    var targetUserId by remember { mutableStateOf("bob") }
    var content by remember { mutableStateOf("Hello Bob") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "TeamFlow WebSocket Slice 01",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(text = "Connection: \${state.status}")

        OutlinedTextField(
            value = userId,
            onValueChange = { userId = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("My userId") },
            singleLine = true
        )

        OutlinedTextField(
            value = targetUserId,
            onValueChange = { targetUserId = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Target userId") },
            singleLine = true
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { chatViewModel.connect(userId) },
                enabled = state.status != ChatStatus.CONNECTED &&
                    state.status != ChatStatus.CONNECTING
            ) {
                Text("Connect")
            }

            Button(
                onClick = { chatViewModel.disconnect() },
                enabled = state.status == ChatStatus.CONNECTED ||
                    state.status == ChatStatus.CONNECTING
            ) {
                Text("Disconnect")
            }
        }

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Message") },
            singleLine = true
        )

        Button(
            onClick = {
                chatViewModel.sendMessage(
                    targetUserId = targetUserId,
                    content = content
                )
            },
            enabled = state.status == ChatStatus.CONNECTED &&
                content.isNotBlank() &&
                targetUserId.isNotBlank()
        ) {
            Text("Send")
        }

        HorizontalDivider()

        Text(
            text = "Messages",
            style = MaterialTheme.typography.titleMedium
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.messages) { message ->
                Text(
                    text = "[\${message.fromUserId}] \${message.content}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        state.error?.let { error ->
            Text(
                text = "Error: \$error",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
