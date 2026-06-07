package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

class DashboardScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<DashboardViewModel>()
        val user by viewModel.currentUser.collectAsState()
        val stats by viewModel.stats.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        var showProfileDialog by remember { mutableStateOf(false) }

        LaunchedEffect(user) {
            if (user == null) {
                navigator.replaceAll(LoginScreen())
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Dashboard") },
                    actions = {
                        Text(
                            text = user?.fullName ?: "",
                            modifier = Modifier.padding(end = 8.dp).clickable { showProfileDialog = true },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (user?.role == com.olivetrust.charity.domain.model.UserRole.APPROVER || 
                            user?.role == com.olivetrust.charity.domain.model.UserRole.SUPER_ADMIN) {
                            IconButton(onClick = { navigator.push(EmployeeManagementScreen()) }) {
                                Text("E")
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                if (user?.role == com.olivetrust.charity.domain.model.UserRole.EMPLOYEE) {
                    FloatingActionButton(onClick = { navigator.push(OnboardingScreen()) }) {
                        Text("+")
                    }
                }
            }
        ) { padding ->
            LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
                item {
                    StatCard("Total Beneficiaries", stats.total.toString(), onClick = {
                        navigator.push(BeneficiaryListScreen())
                    })
                    Spacer(modifier = Modifier.height(8.dp))
                    StatCard("Active", stats.active.toString(), onClick = {
                        navigator.push(BeneficiaryListScreen())
                    })
                    Spacer(modifier = Modifier.height(8.dp))
                    StatCard("Pending Approval", stats.pending.toString(), onClick = {
                        navigator.push(BeneficiaryListScreen())
                    })
                }
            }
        }

        if (showProfileDialog) {
            ProfileDialog(
                user = user,
                onDismiss = { showProfileDialog = false },
                onLogout = {
                    showProfileDialog = false
                    viewModel.logout()
                },
                onUpdateProfile = { name, phone ->
                    viewModel.updateProfile(name, phone)
                }
            )
        }
    }

    @Composable
    private fun ProfileDialog(
        user: com.olivetrust.charity.domain.model.User?,
        onDismiss: () -> Unit,
        onLogout: () -> Unit,
        onUpdateProfile: (String, String) -> Unit
    ) {
        var isEditing by remember { mutableStateOf(false) }
        var name by remember(user) { mutableStateOf(user?.fullName ?: "") }
        var phone by remember(user) { mutableStateOf(user?.mobileNumber ?: "") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Profile") },
            text = {
                Column {
                    if (isEditing) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("Name: ${user?.fullName}")
                        Text("Phone: ${user?.mobileNumber}")
                        Text("Role: ${user?.role}")
                    }
                    
                    if (user?.role != com.olivetrust.charity.domain.model.UserRole.SUPER_ADMIN) {
                        Button(
                            onClick = { 
                                if (isEditing) {
                                    onUpdateProfile(name, phone)
                                    isEditing = false
                                } else {
                                    isEditing = true
                                }
                            },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text(if (isEditing) "Save" else "Edit Details")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onLogout) {
                    Text("Logout", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }

    @Composable
    private fun StatCard(label: String, value: String, onClick: () -> Unit = {}) {
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onClick() }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(label, style = MaterialTheme.typography.labelMedium)
                Text(value, style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}
