package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.olivetrust.charity.domain.model.AidDistribution
import com.olivetrust.charity.domain.model.DeliveryStatus
import kotlin.time.Clock

data class AidDistributionScreen(val beneficiaryId: String, val beneficiaryName: String) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<AidDistributionViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        var natureOfAid by remember { mutableStateOf("") }
        var aidAmount by remember { mutableStateOf("") }
        var packetCount by remember { mutableStateOf("") }
        var reason by remember { mutableStateOf("") }
        var receiverName by remember { mutableStateOf("") }

        LaunchedEffect(state) {
            if (state is AidState.Success) {
                navigator.pop()
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Distribute Aid: $beneficiaryName") })
            }
        ) { padding ->
            LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
                item {
                    OutlinedTextField(
                        value = natureOfAid,
                        onValueChange = { natureOfAid = it },
                        label = { Text("Nature of Aid") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = aidAmount,
                        onValueChange = { aidAmount = it },
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = packetCount,
                        onValueChange = { packetCount = it },
                        label = { Text("Packet Count") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = receiverName,
                        onValueChange = { receiverName = it },
                        label = { Text("Receiver Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (state is AidState.Loading) {
                        CircularProgressIndicator()
                    } else {
                        Button(
                            onClick = {
                                val now = Clock.System.now().toEpochMilliseconds()
                                val distribution = AidDistribution(
                                    distributionId = "D_$now",
                                    date = now,
                                    beneficiaryId = beneficiaryId,
                                    beneficiaryName = beneficiaryName,
                                    areaCode = "",
                                    natureOfAid = natureOfAid,
                                    aidAmount = aidAmount.toDoubleOrNull() ?: 0.0,
                                    packetCount = packetCount.toIntOrNull() ?: 0,
                                    reason = reason,
                                    familyCount = 1,
                                    receiverName = receiverName,
                                    distributedBy = "", 
                                    distributionLocationLat = 0.0,
                                    distributionLocationLng = 0.0,
                                    deliveryStatus = DeliveryStatus.DELIVERED
                                )
                                viewModel.recordDistribution(distribution)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Confirm Delivery")
                        }
                    }

                    if (state is AidState.Error) {
                        Text(
                            text = (state as AidState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }
        }
    }
}
