package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.olivetrust.charity.domain.model.DonationBox
import org.koin.core.parameter.parametersOf

class EditDonationBoxScreen(private val box: DonationBox) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<DonationBoxDetailViewModel> { parametersOf(box.id) }
        val navigator = LocalNavigator.currentOrThrow

        var address by remember { mutableStateOf(box.address) }
        var pocName by remember { mutableStateOf(box.personOfContact) }
        var pocNumber by remember { mutableStateOf(box.contactNumber) }
        var areaCode by remember { mutableStateOf(box.areaCode) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Edit Donation Box") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Physical Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pocName,
                    onValueChange = { pocName = it },
                    label = { Text("Person of Contact") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pocNumber,
                    onValueChange = { pocNumber = it },
                    label = { Text("Contact Number") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                OutlinedTextField(
                    value = areaCode,
                    onValueChange = { areaCode = it },
                    label = { Text("Area Code") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        viewModel.submitUpdate(box.copy(
                            address = address,
                            personOfContact = pocName,
                            contactNumber = pocNumber,
                            areaCode = areaCode
                        ))
                        navigator.pop()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Changes")
                }
            }
        }
    }
}
