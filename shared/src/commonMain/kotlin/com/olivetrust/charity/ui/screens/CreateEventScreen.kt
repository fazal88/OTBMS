package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.olivetrust.charity.domain.model.Beneficiary

class CreateEventScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<CreateEventViewModel>()
        val authViewModel = koinScreenModel<DashboardViewModel>() // To get user ID
        val currentUser by authViewModel.currentUser.collectAsState()
        
        val name by viewModel.name.collectAsState()
        val reason by viewModel.reason.collectAsState()
        val natureOfAid by viewModel.natureOfAid.collectAsState()
        val packetCount by viewModel.packetCount.collectAsState()
        val monetaryAmount by viewModel.monetaryAmount.collectAsState()
        val areaCodeFilter by viewModel.areaCodeFilter.collectAsState()
        val selectedIds by viewModel.selectedBeneficiaryIds.collectAsState()
        val filteredBenes by viewModel.filteredBeneficiaries.collectAsState()
        
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Create Event") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { 
                                currentUser?.let { user ->
                                    viewModel.createEvent(user.userId) {
                                        navigator.pop()
                                    }
                                }
                            },
                            enabled = name.isNotBlank() && reason.isNotBlank() && selectedIds.isNotEmpty()
                        ) {
                            Text("SAVE")
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("Event Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { viewModel.onNameChange(it) },
                        label = { Text("Event Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { viewModel.onReasonChange(it) },
                        label = { Text("Reason/Occasion") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Aid Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = natureOfAid == "Ration", onClick = { viewModel.onNatureOfAidChange("Ration") })
                        Text("Ration")
                        Spacer(Modifier.width(8.dp))
                        RadioButton(selected = natureOfAid == "Monetary", onClick = { viewModel.onNatureOfAidChange("Monetary") })
                        Text("Monetary")
                        Spacer(Modifier.width(8.dp))
                        RadioButton(selected = natureOfAid == "Both", onClick = { viewModel.onNatureOfAidChange("Both") })
                        Text("Both")
                    }
                    
                    if (natureOfAid == "Ration" || natureOfAid == "Both") {
                        OutlinedTextField(
                            value = packetCount?.toString() ?: "",
                            onValueChange = { viewModel.onPacketCountChange(it.toIntOrNull()) },
                            label = { Text("Packet Count") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (natureOfAid == "Monetary" || natureOfAid == "Both") {
                        OutlinedTextField(
                            value = monetaryAmount?.toString() ?: "",
                            onValueChange = { viewModel.onMonetaryAmountChange(it.toDoubleOrNull()) },
                            label = { Text("Amount (Rs)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Invite Beneficiaries (${selectedIds.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { viewModel.selectAllFiltered() }) {
                            Text("Select All Filtered")
                        }
                    }
                    OutlinedTextField(
                        value = areaCodeFilter,
                        onValueChange = { viewModel.onAreaCodeFilterChange(it) },
                        label = { Text("Filter by Area Code") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { Icon(Icons.Default.Search, null) }
                    )
                }

                items(filteredBenes) { bene ->
                    InviteeSelectionItem(
                        beneficiary = bene,
                        isSelected = selectedIds.contains(bene.id),
                        onToggle = { viewModel.toggleBeneficiarySelection(bene.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun InviteeSelectionItem(beneficiary: Beneficiary, isSelected: Boolean, onToggle: () -> Unit) {
    Surface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
            Column {
                Text(beneficiary.headName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("Area: ${beneficiary.areaCode} | Phone: ${beneficiary.phoneNumber}", style = MaterialTheme.typography.bodySmall)
            }
            if (isSelected) {
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
