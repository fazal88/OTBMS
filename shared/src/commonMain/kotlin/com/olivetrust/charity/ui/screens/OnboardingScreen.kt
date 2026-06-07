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
import com.olivetrust.charity.domain.model.Beneficiary
import com.olivetrust.charity.domain.model.BeneficiaryStatus
import kotlin.time.Clock

class OnboardingScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<OnboardingViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        var headName by remember { mutableStateOf("") }
        var phoneNumber by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var incomeSource by remember { mutableStateOf("") }
        var areaCode by remember { mutableStateOf("") }
        var reasonForAid by remember { mutableStateOf("") }
        var numberOfDependants by remember { mutableStateOf("0") }

        LaunchedEffect(state) {
            if (state is OnboardingState.Success) {
                navigator.pop()
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("New Beneficiary") })
            }
        ) { padding ->
            LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
                item {
                    OutlinedTextField(
                        value = headName,
                        onValueChange = { headName = it },
                        label = { Text("Head Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = incomeSource,
                        onValueChange = { incomeSource = it },
                        label = { Text("Income Source") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = areaCode,
                        onValueChange = { areaCode = it },
                        label = { Text("Area Code") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reasonForAid,
                        onValueChange = { reasonForAid = it },
                        label = { Text("Reason for Aid") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = numberOfDependants,
                        onValueChange = { numberOfDependants = it },
                        label = { Text("Number of Dependants") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (state is OnboardingState.Loading) {
                        CircularProgressIndicator()
                    } else {
                        Button(
                            onClick = {
                                val now = Clock.System.now().toEpochMilliseconds()
                                val beneficiary = Beneficiary(
                                    id = "B_$now",
                                    headName = headName,
                                    phoneNumber = phoneNumber,
                                    address = address,
                                    incomeSource = incomeSource,
                                    areaCode = areaCode,
                                    reasonForAid = reasonForAid,
                                    numberOfDependants = numberOfDependants.toIntOrNull() ?: 0,
                                    photoUrl = "", 
                                    natureOfAddress = "Permanent",
                                    onboardingDate = now,
                                    onboardedBy = "", 
                                    latitude = 0.0,
                                    longitude = 0.0,
                                    deviceUsed = "",
                                    status = BeneficiaryStatus.PENDING_APPROVAL
                                )
                                viewModel.submit(beneficiary)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Submit Application")
                        }
                    }

                    if (state is OnboardingState.Error) {
                        Text(
                            text = (state as OnboardingState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }
        }
    }
}
