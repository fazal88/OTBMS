package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.olivetrust.charity.domain.model.*
import kotlin.time.Clock

data class VerificationVisitScreen(val beneficiaryId: String, val beneficiaryName: String) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<VerificationVisitViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(state) {
            if (state is VisitState.Success) {
                navigator.pop()
            }
        }

        VerificationVisitContent(
            beneficiaryName = beneficiaryName,
            state = state,
            onRecord = { status, notes ->
                val now = Clock.System.now().toEpochMilliseconds()
                val visit = VerificationVisit(
                    visitId = "V_$now",
                    date = now,
                    latitude = 0.0,
                    longitude = 0.0,
                    employeeId = "", 
                    beneficiaryId = beneficiaryId,
                    visitStatus = status,
                    misuseReport = if (status == VisitStatus.MISUSE_REPORTED) MisuseReport(notes, "") else null,
                    editRequest = if (status == VisitStatus.EDIT_REQUESTED) EditRequest("", notes) else null,
                    reapprovalReason = if (status == VisitStatus.REAPPROVAL_REQUIRED) notes else null
                )
                viewModel.recordVisit(visit)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationVisitContent(
    beneficiaryName: String,
    state: VisitState,
    onRecord: (VisitStatus, String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var status by remember { mutableStateOf(VisitStatus.SUCCESSFUL) }
    var reportDescription by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            TopAppBar(title = { Text("Visit: $beneficiaryName") })
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            item {
                Text("Visit Result", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                
                VisitStatus.entries.forEach { visitStatus ->
                    Surface(
                        onClick = { status = visitStatus },
                        shape = MaterialTheme.shapes.small,
                        color = if (status == visitStatus) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = status == visitStatus,
                                onClick = { status = visitStatus }
                            )
                            Text(
                                text = when(visitStatus) {
                                    VisitStatus.SUCCESSFUL -> "Successful"
                                    VisitStatus.MISUSE_REPORTED -> "Misuse Reported"
                                    VisitStatus.EDIT_REQUESTED -> "Edit Requested"
                                    VisitStatus.REAPPROVAL_REQUIRED -> "Reapproval Required"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                if (status != VisitStatus.SUCCESSFUL) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = reportDescription,
                        onValueChange = { reportDescription = it },
                        label = { Text("Description / Notes") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                if (state is VisitState.Loading) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = {
                            onRecord(status, reportDescription)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Record Visit")
                    }
                }

                if (state is VisitState.Error) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun VerificationVisitContentPreview() {
    MaterialTheme {
        VerificationVisitContent(
            beneficiaryName = "Muhammad Ahmad",
            state = VisitState.Idle,
            onRecord = { _, _ -> }
        )
    }
}
