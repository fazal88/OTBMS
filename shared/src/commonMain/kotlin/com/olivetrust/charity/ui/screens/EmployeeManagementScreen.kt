package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.olivetrust.charity.domain.model.UserStatus

class EmployeeManagementScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<EmployeeManagementViewModel>()
        val employees by viewModel.employees.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Employee Management") })
            }
        ) { padding ->
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(employees) { employee ->
                    ListItem(
                        headlineContent = { Text(employee.fullName) },
                        supportingContent = { Text("${employee.role} - ${employee.status}") },
                        trailingContent = {
                            Row {
                                if (employee.status != UserStatus.ACTIVE) {
                                    IconButton(onClick = { viewModel.updateStatus(employee.userId, UserStatus.ACTIVE) }) {
                                        Text("A") // Activate
                                    }
                                }
                                if (employee.status != UserStatus.DISABLED) {
                                    IconButton(onClick = { viewModel.updateStatus(employee.userId, UserStatus.DISABLED) }) {
                                        Text("D") // Disable
                                    }
                                }
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
