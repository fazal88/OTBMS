package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.olivetrust.charity.domain.model.Beneficiary
import org.koin.core.parameter.parametersOf

class EventDetailScreen(private val eventId: String) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<EventDetailViewModel> { parametersOf(eventId) }
        val authViewModel = koinScreenModel<DashboardViewModel>()
        val currentUser by authViewModel.currentUser.collectAsState()
        
        val event by viewModel.event.collectAsState()
        val invitees by viewModel.invitees.collectAsState()
        val searchResults by viewModel.searchResults.collectAsState()
        val searchQuery by viewModel.searchQuery.collectAsState()
        
        val navigator = LocalNavigator.currentOrThrow
        var showAddUninvited by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(event?.name ?: "Event Details") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.notifyAllInvitees() }) {
                            Icon(Icons.Default.Email, contentDescription = "Notify All")
                        }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Event Info Header
                event?.let { e ->
                    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(e.reason, style = MaterialTheme.typography.titleMedium)
                            Text("Type: ${e.natureOfAid}", style = MaterialTheme.typography.bodyMedium)
                            Text("Aided: ${invitees.count { it.hasReceivedAid }} / ${invitees.size}", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Invitee List", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Button(onClick = { showAddUninvited = true }) {
                        Icon(Icons.Default.Person, null)
                        Text("Add Uninvited")
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(invitees) { status ->
                        InviteeRow(
                            status = status,
                            onMarkDistributed = { 
                                currentUser?.let { user ->
                                    viewModel.recordAid(status.beneficiary, user.userId)
                                }
                            }
                        )
                    }
                }
            }
        }

        if (showAddUninvited) {
            AlertDialog(
                onDismissRequest = { showAddUninvited = false },
                title = { Text("Add Uninvited Beneficiary") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            label = { Text("Search by Name or Phone") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyColumn(modifier = Modifier.height(200.dp)) {
                            items(searchResults) { bene ->
                                ListItem(
                                    headlineContent = { Text(bene.headName) },
                                    supportingContent = { Text(bene.phoneNumber) },
                                    trailingContent = {
                                        Button(onClick = { 
                                            currentUser?.let { user ->
                                                viewModel.addUninvitedAndDistribute(bene, user.userId)
                                                showAddUninvited = false
                                            }
                                        }) {
                                            Text("Add & Aid")
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAddUninvited = false }) { Text("Close") }
                }
            )
        }
    }
}

@Composable
fun InviteeRow(status: InviteeStatus, onMarkDistributed: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (status.hasReceivedAid) 
            CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)) 
            else CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(status.beneficiary.headName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("Phone: ${status.beneficiary.phoneNumber}", style = MaterialTheme.typography.bodySmall)
            }
            if (status.hasReceivedAid) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Aided", tint = Color(0xFF4CAF50))
                Spacer(Modifier.width(8.dp))
                Text("Aided", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
            } else {
                Button(onClick = onMarkDistributed) {
                    Text("Mark Distributed")
                }
            }
        }
    }
}
