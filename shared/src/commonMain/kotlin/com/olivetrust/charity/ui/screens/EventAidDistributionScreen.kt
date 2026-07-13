package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.olivetrust.charity.domain.model.AidDistribution
import com.olivetrust.charity.domain.model.DeliveryStatus
import org.koin.core.parameter.parametersOf
import kotlin.time.Clock

data class EventAidDistributionScreen(val eventId: String, val beneficiaryId: String) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel =
            koinScreenModel<EventAidDistributionViewModel> { parametersOf(eventId, beneficiaryId) }
        val state by viewModel.state.collectAsState()
        val event by viewModel.event.collectAsState()
        val beneficiary by viewModel.beneficiary.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val focusManager = LocalFocusManager.current

        var receiverName by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        LaunchedEffect(beneficiary) {
            if (receiverName.isEmpty() && beneficiary != null) {
                receiverName = beneficiary?.headName ?: ""
            }
        }

        LaunchedEffect(state) {
            if (state is AidState.Success) {
                navigator.pop()
            }
        }

        Scaffold(
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            },
            topBar = {
                TopAppBar(
                    title = { Text("Event Aid Distribution") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            if (event == null || beneficiary == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Confirm distribution for:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = beneficiary?.headName ?: "",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                                    alpha = 0.5f
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Event Aid Details",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))

                                Text(
                                    event!!.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(event!!.reason, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(8.dp))

                                if (event!!.aidDescription.isNotBlank()) {
                                    EventAidDetailRow("Aid", event!!.aidDescription)
                                } else {
                                    EventAidDetailRow("Nature", event!!.natureOfAid)
                                    if (event!!.packetCount != null && event!!.packetCount!! > 0) {
                                        EventAidDetailRow("Packets", event!!.packetCount.toString())
                                    }
                                    if (event!!.monetaryAidAmount != null && event!!.monetaryAidAmount!! > 0) {
                                        EventAidDetailRow(
                                            "Amount",
                                            "₹ ${event!!.monetaryAidAmount}"
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = receiverName,
                            onValueChange = { receiverName = it },
                            label = { Text("Receiver Name (who is collecting?)") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Person, null) }
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes / Observations") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 3
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))

                        if (state is AidState.Loading) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            Button(
                                onClick = {
                                    val now = Clock.System.now().toEpochMilliseconds()
                                    val distribution = AidDistribution(
                                        distributionId = "ED_${now}_${beneficiary?.id}",
                                        date = now,
                                        beneficiaryId = beneficiary?.id ?: "",
                                        beneficiaryName = beneficiary?.headName ?: "",
                                        areaCode = beneficiary?.areaCode ?: "",
                                        natureOfAid = if (event!!.aidDescription.isNotBlank()) event!!.aidDescription else event!!.natureOfAid,
                                        aidAmount = event!!.monetaryAidAmount ?: 0.0,
                                        packetCount = event!!.packetCount ?: 0,
                                        reason = event!!.reason,
                                        familyCount = beneficiary?.numberOfDependants ?: 0 + 1,
                                        receiverName = receiverName,
                                        distributedBy = "",
                                        distributionLocationLat = 0.0,
                                        distributionLocationLng = 0.0,
                                        deliveryStatus = DeliveryStatus.DELIVERED,
                                        eventId = eventId
                                    )
                                    viewModel.recordDistribution(distribution)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(16.dp)
                            ) {
                                Icon(Icons.Default.Check, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Confirm Event Delivery")
                            }
                        }

                        if (state is AidState.Error) {
                            Text(
                                text = (state as AidState.Error).message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventAidDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
