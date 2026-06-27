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
                    aidAmount = if (nature != "Ration") (amount.toDoubleOrNull() ?: 0.0) else 0.0,
                    packetCount = if (nature == "Ration" || nature == "Both") (packets.toIntOrNull() ?: 0) else 0,
                    reason = reason,
                    familyCount = beneficiary?.numberOfDependants?.plus(1) ?: 1,
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
    onConfirm: (String, String, String, String, String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var natureOfAid by remember { mutableStateOf("Ration") }
    var aidAmount by remember { mutableStateOf("") }
    var packetCount by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var receiverName by remember { mutableStateOf("") }
    var natureExpanded by remember { mutableStateOf(false) }

    // Pre-fill logic when beneficiary is loaded
    LaunchedEffect(beneficiary) {
        beneficiary?.let { b ->
            if (b.natureOfAid != null) natureOfAid = b.natureOfAid
            if (b.monetaryAidAmount != null && b.monetaryAidAmount > 0) aidAmount = b.monetaryAidAmount.toString()
            if (b.packetCount != null && b.packetCount > 0) packetCount = b.packetCount.toString()
//            if (receiverName.isEmpty()) receiverName = b.headName
        }
    }
    
    val natureOptions = listOf("Ration", "Monetary", "Medical", "Emergency", "Other")

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            TopAppBar(
                title = { Text("Distribute Aid") },
                navigationIcon = {
                    // Back button should be handled by navigator, but adding for completeness
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = beneficiaryName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                ExposedDropdownMenuBox(
                    expanded = natureExpanded,
                    onExpandedChange = { natureExpanded = !natureExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = natureOfAid,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Nature of Aid") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = natureExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = natureExpanded,
                        onDismissRequest = { natureExpanded = false }
                    ) {
                        natureOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    natureOfAid = option
                                    if (option == "Monetary") {
                                        packetCount = ""
                                    } else if (option == "Ration") {
                                        aidAmount = ""
                                    }
                                    natureExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = aidAmount,
                    onValueChange = { aidAmount = it },
                    label = { Text("Amount (in ₹)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.CheckCircle, null) }
                )
            }

            item {
                OutlinedTextField(
                    value = packetCount,
                    onValueChange = { packetCount = it },
                    label = { Text("Packet Count (if ration)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.ShoppingCart, null) }
                )
            }

            item {
                OutlinedTextField(
                    value = receiverName,
                    onValueChange = { receiverName = it },
                    label = { Text("Receiver Name") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Person, null) }
                )
            }

            item {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Notes / Reason") },
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
                            onConfirm(natureOfAid, aidAmount, packetCount, reason, receiverName)
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

@Preview
@Composable
fun AidDistributionContentPreview() {
    MaterialTheme {
        AidDistributionContent(
            beneficiaryName = "Muhammad Ahmad",
            beneficiary = null,
            state = AidState.Idle,
            onConfirm = { _, _, _, _, _ -> }
        )
    }
}
