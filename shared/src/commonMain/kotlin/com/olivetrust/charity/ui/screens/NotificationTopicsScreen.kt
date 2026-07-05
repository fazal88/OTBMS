package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.olivetrust.charity.domain.model.NotificationTopic

class NotificationTopicsScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<NotificationTopicsViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        var showAddTopicDialog by remember { mutableStateOf(false) }
        var topicToEdit by remember { mutableStateOf<NotificationTopic?>(null) }
        var showDeleteConfirm by remember { mutableStateOf<String?>(null) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Notification Topics", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAddTopicDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Topic")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        DeveloperInfoCard(
                            token = state.fcmToken ?: "Fetching...",
                            env = state.environment
                        )
                    }

                    item {
                        Text(
                            "Available Topics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    if (state.topics.isEmpty() && !state.isLoading) {
                        item {
                            EmptyStateMessage("No topics available. Add one to get started.")
                        }
                    }

                    items(state.topics) { topic ->
                        TopicItem(
                            topic = topic,
                            isSubscribed = state.subscribedTopicIds.contains(topic.topicId),
                            onSubscribe = { viewModel.subscribe(topic) },
                            onUnsubscribe = { viewModel.unsubscribe(topic) },
                            onEdit = { topicToEdit = topic },
                            onDelete = { showDeleteConfirm = topic.topicId },
                            onTest = { viewModel.sendTestNotification(topic) }
                        )
                    }

                    item {
                        Text(
                            "Notification Logs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    if (state.logs.isEmpty()) {
                        item {
                            EmptyStateMessage("No logs available.")
                        }
                    }

                    items(state.logs) { log ->
                        LogItem(log)
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }

                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }

        // Dialogs
        if (showAddTopicDialog) {
            TopicDialog(
                onDismiss = { showAddTopicDialog = false },
                onConfirm = { name, display, desc ->
                    viewModel.createTopic(name, display, desc)
                    showAddTopicDialog = false
                }
            )
        }

        topicToEdit?.let { topic ->
            TopicDialog(
                topic = topic,
                onDismiss = { topicToEdit = null },
                onConfirm = { name, display, desc ->
                    viewModel.updateTopic(topic.copy(name = name, displayName = display, description = desc))
                    topicToEdit = null
                }
            )
        }

        showDeleteConfirm?.let { topicId ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = null },
                title = { Text("Delete Topic") },
                text = { Text("Are you sure you want to delete this topic? This cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteTopic(topicId)
                            showDeleteConfirm = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        state.error?.let { error ->
            LaunchedEffect(error) {
                // In a real app, use a Snackbar
                println("NOTIFICATION_ERROR: $error")
                viewModel.clearError()
            }
        }
    }
}

@Composable
fun DeveloperInfoCard(token: String, env: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Developer Info", fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (env == "PRODUCTION") Color(0xFF386B1D) else Color(0xFFB36200)
                ) {
                    Text(
                        env,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("FCM Token", style = MaterialTheme.typography.labelSmall)
            Text(
                token,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun LogItem(log: com.olivetrust.charity.domain.model.NotificationLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    log.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (log.status == "SUCCESS") Color(0xFF386B1D) else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    log.topic,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(log.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(log.body, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun TopicItem(
    topic: NotificationTopic,
    isSubscribed: Boolean,
    onSubscribe: () -> Unit,
    onUnsubscribe: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(topic.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(topic.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                
                Switch(
                    checked = isSubscribed,
                    onCheckedChange = { if (it) onSubscribe() else onUnsubscribe() }
                )
            }
            
            Spacer(Modifier.height(8.dp))
            Text(topic.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(Modifier.height(12.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onTest,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Send, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Test", fontSize = 12.sp)
                }
                
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
                
                if (!topic.isSystemTopic) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun TopicDialog(
    topic: NotificationTopic? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(topic?.name ?: "") }
    var display by remember { mutableStateOf(topic?.displayName ?: "") }
    var desc by remember { mutableStateOf(topic?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (topic == null) "Add Topic" else "Edit Topic") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = display,
                    onValueChange = { display = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = topic?.isSystemTopic != true
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Topic Name (FCM)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = topic == null
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, display, desc) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EmptyStateMessage(message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Notifications, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
    }
}
