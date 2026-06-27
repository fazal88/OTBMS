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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.olivetrust.charity.domain.model.AidDistribution
import com.olivetrust.charity.domain.model.Beneficiary
import com.olivetrust.charity.domain.model.DeliveryStatus
import kotlin.time.Clock

data class AidDistributionScreen(val beneficiaryId: String, val beneficiaryName: String) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<AidDistributionViewModel>()
        val state by viewModel.state.collectAsState()
        val beneficiary by viewModel.beneficiary.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(beneficiaryId) {
            viewModel.loadBeneficiary(beneficiaryId)
        }

        LaunchedEffect(state) {
            if (state is AidState.Success) {
                navigator.pop()
            }
        }

        AidDistributionContent(
            beneficiaryName = beneficiaryName,
            beneficiary = beneficiary,
            state = state,
            onBack = { navigator.pop() },
            onConfirm = { nature, amount, packets, reason, receiver ->
                val now = Clock.System.now().toEpochMilliseconds()
                val finalReceiver = if (receiver.isBlank()) beneficiaryName else receiver
                val distribution = AidDistribution(
                    distributionId = "D_$now",
                    date = now,
                    beneficiaryId = beneficiaryId,
                    beneficiaryName = beneficiaryName,
                    areaCode = beneficiary?.areaCode ?: "",
                    natureOfAid = nature,
                    aidAmount = amount.toDoubleOrNull() ?: 0.0,
                    packetCount = packets.toIntOrNull() ?: 0,
                    reason = reason,
                    familyCount = beneficiary?.familyMembers?.size?.plus(1) ?: 1,
                    receiverName = finalReceiver,
                    distributedBy = "", 
                    distributionLocationLat = 0.0,
                    distributionLocationLng = 0.0,
                    deliveryStatus = DeliveryStatus.DELIVERED
                )
                viewModel.recordDistribution(distribution)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AidDistributionContent(
    beneficiaryName: String,
    beneficiary: Beneficiary?,
    state: AidState,
    onBack: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    
    // We pre-fill everything from approved data and don't allow editing (as requested)
    var receiverName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Pre-fill receiver name if needed
    LaunchedEffect(beneficiary) {
        if (receiverName.isEmpty() && beneficiary != null) {
            receiverName = beneficiary.headName
        }
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            TopAppBar(
                title = { Text("Distribute Aid") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (beneficiary == null) {
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
                        text = beneficiaryName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Approved Aid Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            
                            ApprovedAidDetailRow("Nature", beneficiary.natureOfAid ?: "N/A")
                            if (beneficiary.monthlyRation != null) {
                                ApprovedAidDetailRow("Ration", beneficiary.monthlyRation)
                            }
                            if (beneficiary.packetCount != null && beneficiary.packetCount > 0) {
                                ApprovedAidDetailRow("Packets", beneficiary.packetCount.toString())
                            }
                            if (beneficiary.monetaryAidAmount != null && beneficiary.monetaryAidAmount > 0) {
                                ApprovedAidDetailRow("Amount", "₹ ${beneficiary.monetaryAidAmount}")
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
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Button(
                            onClick = {
                                onConfirm(
                                    beneficiary.natureOfAid ?: "Other",
                                    beneficiary.monetaryAidAmount?.toString() ?: "0",
                                    beneficiary.packetCount?.toString() ?: "0",
                                    notes,
                                    receiverName
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Icon(Icons.Default.Check, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirm Delivery")
                        }
                    }

                    if (state is AidState.Error) {
                        Text(
                            text = state.message,
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

@Composable
private fun ApprovedAidDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Preview
@Composable
fun AidDistributionContentPreview() {
    MaterialTheme {
        AidDistributionContent(
            beneficiaryName = "Muhammad Ahmad",
            beneficiary = null,
            state = AidState.Idle,
            onBack = {},
            onConfirm = { _, _, _, _, _ -> }
        )
    }
}
