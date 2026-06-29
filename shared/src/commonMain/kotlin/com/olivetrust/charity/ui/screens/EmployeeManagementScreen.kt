package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.olivetrust.charity.domain.model.User
import com.olivetrust.charity.domain.model.UserStatus
import com.olivetrust.charity.ui.previews.PreviewMocks

class EmployeeManagementScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<EmployeeManagementViewModel>()
        val employees by viewModel.employees.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        EmployeeManagementContent(
            employees = employees,
            onUpdateStatus = { userId, status -> viewModel.updateStatus(userId, status) },
            onBack = { navigator.pop() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeManagementContent(
    employees: List<User>,
    onUpdateStatus: (String, UserStatus) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Employee Management") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
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
                                Button(
                                    onClick = { onUpdateStatus(employee.userId, UserStatus.ACTIVE) },
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Text("Activate")
                                }
                            }
                            if (employee.status != UserStatus.DISABLED) {
                                OutlinedButton(onClick = { onUpdateStatus(employee.userId, UserStatus.DISABLED) }) {
                                    Text("Disable")
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

@Preview
@Composable
fun EmployeeManagementContentPreview() {
    MaterialTheme {
        EmployeeManagementContent(
            employees = listOf(
                PreviewMocks.mockUser,
                PreviewMocks.mockUser.copy(userId = "2", fullName = "Jane Smith", status = UserStatus.DISABLED)
            ),
            onUpdateStatus = { _, _ -> },
            onBack = {}
        )
    }
}
