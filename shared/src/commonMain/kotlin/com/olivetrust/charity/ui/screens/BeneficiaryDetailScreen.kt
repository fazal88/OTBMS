package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.olivetrust.charity.domain.model.Beneficiary
import com.olivetrust.charity.domain.model.BeneficiaryStatus
import com.olivetrust.charity.domain.repository.BeneficiaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BeneficiaryDetailScreen(private val beneficiaryId: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<BeneficiaryDetailViewModel>()
        val beneficiary by viewModel.beneficiary.collectAsState()

        LaunchedEffect(beneficiaryId) {
            viewModel.loadBeneficiary(beneficiaryId)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Beneficiary Details") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        beneficiary?.let {
                            if (it.status == BeneficiaryStatus.PENDING_APPROVAL) {
                                TextButton(onClick = { navigator.push(EditBeneficiaryScreen(it)) }) {
                                    Text("Edit")
                                }
                                TextButton(
                                    onClick = {
                                        viewModel.deleteBeneficiary(it.id) {
                                            navigator.pop()
                                        }
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                )
            }
        ) { padding ->
            beneficiary?.let { b ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        SectionHeader("Status")
                        StatusBadge(b.status)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item {
                        SectionHeader("Head of Family")
                        DetailRow("Name", b.headName)
                        DetailRow("Age", b.headAge.toString())
                        DetailRow("Gender", b.headGender)
                        DetailRow("Occupation", b.headOccupation)
                        DetailRow("Education", b.headEducation)
                        DetailRow("Phone", b.phoneNumber)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item {
                        SectionHeader("Address & Background")
                        DetailRow("Address", b.address)
                        DetailRow("Area Code", b.areaCode)
                        DetailRow("Nature of Address", b.natureOfAddress)
                        b.natureOfRent?.let { DetailRow("Rent Details", it) }
                        DetailRow("Income Source", b.incomeSource)
                        b.diseaseInability?.let { DetailRow("Disease/Inability", it) }
                        DetailRow("Reason for Aid", b.reasonForAid)
                        DetailRow("Dependants", b.numberOfDependants.toString())
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (b.familyMembers.isNotEmpty()) {
                        item {
                            SectionHeader("Family Members")
                        }
                        items(b.familyMembers) { member ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("${member.relation}: ${member.name}", fontWeight = FontWeight.Bold)
                                    Text("Age: ${member.age} | Gender: ${member.gender}")
                                    Text("Occupation: ${member.occupation}")
                                    Text("Education: ${member.education}")
                                    member.diseaseInability?.let { Text("Disease: $it") }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader("Metadata")
                        DetailRow("Onboarded By", b.onboardedBy)
                        DetailRow("Device Used", b.deviceUsed)
                        DetailRow("Latitude", b.latitude.toString())
                        DetailRow("Longitude", b.longitude.toString())
                    }
                }
            } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }

    @Composable
    private fun SectionHeader(title: String) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }

    @Composable
    private fun DetailRow(label: String, value: String) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }

    @Composable
    private fun StatusBadge(status: BeneficiaryStatus) {
        val color = when (status) {
            BeneficiaryStatus.APPROVED -> MaterialTheme.colorScheme.primary
            BeneficiaryStatus.PENDING_APPROVAL -> MaterialTheme.colorScheme.secondary
            BeneficiaryStatus.REJECTED -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.outline
        }
        Surface(
            color = color.copy(alpha = 0.1f),
            shape = MaterialTheme.shapes.small,
            border = androidx.compose.foundation.BorderStroke(1.dp, color)
        ) {
            Text(
                text = status.name,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
        }
    }
}

class BeneficiaryDetailViewModel(
    private val repository: BeneficiaryRepository
) : ScreenModel {
    private val _beneficiary = MutableStateFlow<Beneficiary?>(null)
    val beneficiary: StateFlow<Beneficiary?> = _beneficiary.asStateFlow()

    fun loadBeneficiary(id: String) {
        screenModelScope.launch {
            repository.getBeneficiaryById(id).collect {
                _beneficiary.value = it
            }
        }
    }

    fun deleteBeneficiary(id: String, onDeleted: () -> Unit) {
        screenModelScope.launch {
            val result = repository.deleteBeneficiary(id)
            if (result.isSuccess) {
                onDeleted()
            }
        }
    }
}
