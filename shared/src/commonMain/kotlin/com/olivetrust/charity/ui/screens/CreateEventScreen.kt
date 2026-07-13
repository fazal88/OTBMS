package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
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
        val aidDescription by viewModel.aidDescription.collectAsState()
        val areaCodeFilter by viewModel.areaCodeFilter.collectAsState()
        val selectedIds by viewModel.selectedBeneficiaryIds.collectAsState()
        val filteredBenes by viewModel.filteredBeneficiaries.collectAsState()
        
        val navigator = LocalNavigator.currentOrThrow
        val focusManager = LocalFocusManager.current

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
                            enabled = name.isNotBlank() && reason.isNotBlank() && aidDescription.isNotBlank() && selectedIds.isNotEmpty()
                        ) {
                            Text("SAVE")
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { focusManager.clearFocus() })
                    },
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
                    Text("Aid Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = aidDescription,
                        onValueChange = { viewModel.onAidDescriptionChange(it) },
                        label = { Text("Aid Description") },
                        placeholder = { Text("e.g. 5kg Ration Packet + 2000 Cash") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
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
