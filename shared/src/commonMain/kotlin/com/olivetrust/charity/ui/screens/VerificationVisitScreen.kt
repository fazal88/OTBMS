package com.olivetrust.charity.ui.screens

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
import com.olivetrust.charity.domain.model.*
import kotlin.time.Clock

data class VerificationVisitScreen(val beneficiaryId: String, val beneficiaryName: String) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<VerificationVisitViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        var status by remember { mutableStateOf(VisitStatus.SUCCESSFUL) }
        var reportDescription by remember { mutableStateOf("") }

        LaunchedEffect(state) {
            if (state is VisitState.Success) {
                navigator.pop()
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Visit: $beneficiaryName") })
            }
        ) { padding ->
            LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
                item {
                    Text("Visit Status", style = MaterialTheme.typography.labelLarge)
                    VisitStatus.entries.forEach { visitStatus ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            RadioButton(
                                selected = status == visitStatus,
                                onClick = { status = visitStatus }
                            )
                            Text(visitStatus.name, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    
                    if (status != VisitStatus.SUCCESSFUL) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = reportDescription,
                            onValueChange = { reportDescription = it },
                            label = { Text("Description / Notes") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (state is VisitState.Loading) {
                        CircularProgressIndicator()
                    } else {
                        Button(
                            onClick = {
                                val now = Clock.System.now().toEpochMilliseconds()
                                val visit = VerificationVisit(
                                    visitId = "V_$now",
                                    date = now,
                                    latitude = 0.0,
                                    longitude = 0.0,
                                    employeeId = "", 
                                    beneficiaryId = beneficiaryId,
                                    visitStatus = status,
                                    misuseReport = if (status == VisitStatus.MISUSE_REPORTED) MisuseReport(reportDescription, "") else null,
                                    editRequest = if (status == VisitStatus.EDIT_REQUESTED) EditRequest("", reportDescription) else null,
                                    reapprovalReason = if (status == VisitStatus.REAPPROVAL_REQUIRED) reportDescription else null
                                )
                                viewModel.recordVisit(visit)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Record Visit")
                        }
                    }

                    if (state is VisitState.Error) {
                        Text(
                            text = (state as VisitState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }
        }
    }
}
