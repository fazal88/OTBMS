package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
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
        val focusManager = LocalFocusManager.current

        var address by remember { mutableStateOf(box.address) }
        var pocName by remember { mutableStateOf(box.personOfContact) }
        var pocNumber by remember { mutableStateOf(box.contactNumber.filter { it.isDigit() }.take(10)) }
        var areaCode by remember { mutableStateOf(box.areaCode) }

        val updateSuccess by viewModel.updateSuccess.collectAsState()
        val isProcessing by viewModel.isProcessing.collectAsState()
        val error by viewModel.error.collectAsState()

        LaunchedEffect(updateSuccess) {
            if (updateSuccess) {
                viewModel.resetUpdateSuccess()
                navigator.pop()
            }
        }

        Scaffold(
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            },
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
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = pocName,
                    onValueChange = { pocName = it },
                    label = { Text("Person of Contact") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = pocNumber,
                    onValueChange = { 
                        val digits = it.filter { char -> char.isDigit() }
                        if (digits.length <= 10) {
                            pocNumber = digits
                        }
                    },
                    label = { Text("Contact Number (10 digits)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp),
                    isError = pocNumber.length != 10 && pocNumber.isNotEmpty()
                )
                OutlinedTextField(
                    value = areaCode,
                    onValueChange = { areaCode = it },
                    label = { Text("Area Code") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                val currentBoxState by viewModel.box.collectAsState()

        Button(
            onClick = {
                focusManager.clearFocus()
                val boxToUpdate = currentBoxState ?: box
                viewModel.submitUpdate(boxToUpdate.copy(
                    address = address,
                    personOfContact = pocName,
                    contactNumber = pocNumber,
                    areaCode = areaCode
                ))
            },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isProcessing && 
                            address.isNotBlank() && 
                            pocName.isNotBlank() && 
                            pocNumber.length == 10
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
