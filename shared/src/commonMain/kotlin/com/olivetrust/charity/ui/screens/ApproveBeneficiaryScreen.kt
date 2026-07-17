package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.olivetrust.charity.domain.model.User
import com.olivetrust.charity.domain.repository.AuthRepository
import com.olivetrust.charity.domain.repository.BeneficiaryRepository
import com.olivetrust.charity.domain.repository.EmployeeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ApproveBeneficiaryScreen(private val beneficiaryId: String, private val beneficiaryName: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<ApproveBeneficiaryViewModel>()
        val monitors by viewModel.monitors.collectAsState()
        val state by viewModel.state.collectAsState()
        val focusManager = LocalFocusManager.current

        LaunchedEffect(state) {
            if (state is ApprovalState.Success) {
                navigator.pop()
            }
        }

        Scaffold(
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            },
            topBar = {
                TopAppBar(
                    title = { Text("Approve: $beneficiaryName") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                var notes by remember { mutableStateOf("") }
                var natureOfAid by remember { mutableStateOf("Ration") }
                var monthlyRation by remember { mutableStateOf("") }
                var packetCount by remember { mutableStateOf("") }
                var monetaryAidAmount by remember { mutableStateOf("") }
                var medicalAidAmount by remember { mutableStateOf("") }
                var educationAidAmount by remember { mutableStateOf("") }
                var selectedMonitorId by remember { mutableStateOf("") }
                var monitorExpanded by remember { mutableStateOf(false) }

                var expiryMonth by remember { mutableStateOf("") }
                var expiryYear by remember { mutableStateOf("") }
                var expiryMonthExpanded by remember { mutableStateOf(false) }

                Text("Aid Approval Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Approval Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                Text("Nature of Aid", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                // Row 1: Ration, Monetary, Both
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Ration", "Monetary", "Both").forEach { option ->
                        FilterChip(
                            selected = natureOfAid == option,
                            onClick = { 
                                natureOfAid = option
                                when (option) {
                                    "Monetary" -> {
                                        monthlyRation = ""
                                        packetCount = ""
                                        medicalAidAmount = ""
                                        educationAidAmount = ""
                                    }
                                    "Ration" -> {
                                        monetaryAidAmount = ""
                                        medicalAidAmount = ""
                                        educationAidAmount = ""
                                    }
                                    "Both" -> {
                                        medicalAidAmount = ""
                                        educationAidAmount = ""
                                    }
                                }
                            },
                            label = { Text(option) }
                        )
                    }
                }
                // Row 2: Medical, Education
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Medical", "Education").forEach { option ->
                        FilterChip(
                            selected = natureOfAid == option,
                            onClick = {
                                natureOfAid = option
                                monthlyRation = ""
                                packetCount = ""
                                monetaryAidAmount = ""
                                when (option) {
                                    "Medical" -> educationAidAmount = ""
                                    "Education" -> medicalAidAmount = ""
                                }
                            },
                            label = { Text(option) }
                        )
                    }
                }

                if (natureOfAid == "Ration" || natureOfAid == "Both") {
                    OutlinedTextField(
                        value = monthlyRation,
                        onValueChange = { monthlyRation = it },
                        label = { Text("Monthly Ration Details") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    OutlinedTextField(
                        value = packetCount,
                        onValueChange = { packetCount = it },
                        label = { Text("Packet Count") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                    )
                }

                if (natureOfAid == "Monetary" || natureOfAid == "Both") {
                    OutlinedTextField(
                        value = monetaryAidAmount,
                        onValueChange = { monetaryAidAmount = it },
                        label = { Text("Monetary Aid Amount (in ₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                    )
                }

                if (natureOfAid == "Medical") {
                    OutlinedTextField(
                        value = medicalAidAmount,
                        onValueChange = { medicalAidAmount = it },
                        label = { Text("Medical Aid Amount (in ₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                    )
                }

                if (natureOfAid == "Education") {
                    OutlinedTextField(
                        value = educationAidAmount,
                        onValueChange = { educationAidAmount = it },
                        label = { Text("Education Aid Amount (in ₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                    )
                }

                Text("Aid Expiry (Last month/year of aid)", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = if (expiryMonth.isBlank()) "month" else getMonthName(expiryMonth.toInt()),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Expiry Month") },
                            trailingIcon = { IconButton(onClick = { expiryMonthExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, null)
                            }},
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = expiryMonthExpanded,
                            onDismissRequest = { expiryMonthExpanded = false }
                        ) {
                            (1..12).forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(getMonthName(m)) },
                                    onClick = {
                                        expiryMonth = m.toString()
                                        expiryMonthExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = expiryYear,
                        onValueChange = { expiryYear = it },
                        label = { Text("Expiry Year") },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("e.g. 2025") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() })
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = monitorExpanded,
                    onExpandedChange = { monitorExpanded = it }
                ) {
                    OutlinedTextField(
                        value = monitors.find { it.userId == selectedMonitorId }?.fullName ?: "Select Monitor",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assign Monitor") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monitorExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = monitorExpanded,
                        onDismissRequest = { monitorExpanded = false }
                    ) {
                        monitors.forEach { monitor ->
                            DropdownMenuItem(
                                text = { Text(monitor.fullName) },
                                onClick = {
                                    selectedMonitorId = monitor.userId
                                    monitorExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (state is ApprovalState.Error) {
                    Text(
                        text = (state as ApprovalState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.approve(
                            id = beneficiaryId,
                            notes = notes,
                            natureOfAid = natureOfAid,
                            monthlyRation = if (natureOfAid == "Ration" || natureOfAid == "Both") monthlyRation.ifBlank { null } else null,
                            packetCount = if (natureOfAid == "Ration" || natureOfAid == "Both") packetCount.toIntOrNull() else null,
                            monetaryAidAmount = if (natureOfAid == "Monetary" || natureOfAid == "Both") monetaryAidAmount.toDoubleOrNull() else null,
                            medicalAidAmount = if (natureOfAid == "Medical") medicalAidAmount.toDoubleOrNull() else null,
                            educationAidAmount = if (natureOfAid == "Education") educationAidAmount.toDoubleOrNull() else null,
                            monitorId = selectedMonitorId,
                            expiryMonth = expiryMonth.toIntOrNull(),
                            expiryYear = expiryYear.toIntOrNull()
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedMonitorId.isNotBlank() && state !is ApprovalState.Loading
                ) {
                    if (state is ApprovalState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Confirm Approval")
                    }
                }
            }
        }
    }
}

class ApproveBeneficiaryViewModel(
    private val beneficiaryRepository: BeneficiaryRepository,
    employeeRepository: EmployeeRepository,
    private val authRepository: AuthRepository
) : ScreenModel {

    private val _state = MutableStateFlow<ApprovalState>(ApprovalState.Idle)
    val state: StateFlow<ApprovalState> = _state.asStateFlow()

    val currentUser = authRepository.currentUser.stateIn(
        screenModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    val monitors: StateFlow<List<User>> = employeeRepository.getEmployees()
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun approve(
        id: String,
        notes: String,
        natureOfAid: String,
        monthlyRation: String?,
        packetCount: Int?,
        monetaryAidAmount: Double?,
        medicalAidAmount: Double?,
        educationAidAmount: Double?,
        monitorId: String,
        expiryMonth: Int?,
        expiryYear: Int?
    ) {
        screenModelScope.launch {
            _state.value = ApprovalState.Loading
            val approverId = currentUser.value?.userId ?: ""
            val result = beneficiaryRepository.approveBeneficiary(
                id = id,
                approverId = approverId,
                notes = notes,
                natureOfAid = natureOfAid,
                monthlyRation = monthlyRation,
                packetCount = packetCount,
                monetaryAidAmount = monetaryAidAmount,
                medicalAidAmount = medicalAidAmount,
                educationAidAmount = educationAidAmount,
                monitorId = monitorId,
                expiryMonth = expiryMonth,
                expiryYear = expiryYear
            )
            if (result.isSuccess) {
                _state.value = ApprovalState.Success
            } else {
                _state.value = ApprovalState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }
}

sealed class ApprovalState {
    object Idle : ApprovalState()
    object Loading : ApprovalState()
    object Success : ApprovalState()
    data class Error(val message: String) : ApprovalState()
}

private fun getMonthName(month: Int): String {
    return when (month) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> ""
    }
}
