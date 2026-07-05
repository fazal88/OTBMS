package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.olivetrust.charity.domain.model.User

class EmployeeManagementScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<EmployeeManagementViewModel>()
        val employees by viewModel.employees.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        var employeeToDelete by remember { mutableStateOf<User?>(null) }

        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text("User Management") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { navigator.push(EditUserScreen()) }) {
                    Icon(Icons.Default.Add, contentDescription = "Add User")
                }
            }
        ) { padding ->
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(employees) { employee ->
                    ListItem(
                        headlineContent = { Text(employee.fullName) },
                        supportingContent = { 
                            Column {
                                Text("Username: ${employee.username}")
                                Text("Role: ${employee.role} | Status: ${employee.status}")
                            }
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { navigator.push(EditUserScreen(employee)) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { employeeToDelete = employee }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }

            employeeToDelete?.let { employee ->
                AlertDialog(
                    onDismissRequest = { employeeToDelete = null },
                    title = { Text("Delete User") },
                    text = { Text("Are you sure you want to delete ${employee.fullName}?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteEmployee(employee.userId)
                                employeeToDelete = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { employeeToDelete = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
