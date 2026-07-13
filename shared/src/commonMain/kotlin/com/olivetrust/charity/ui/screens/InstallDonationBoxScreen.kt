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
import com.olivetrust.charity.ui.components.LocationSelectionComponent

class InstallDonationBoxScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<InstallDonationBoxViewModel>()
        val isSubmitting by viewModel.isSubmitting.collectAsState()
        val error by viewModel.error.collectAsState()
        val success by viewModel.success.collectAsState()
        val selectedLocation by viewModel.selectedLocation.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val focusManager = LocalFocusManager.current

        LaunchedEffect(success) {
            if (success) {
                navigator.pop()
            }
        }

        var address by remember { mutableStateOf("") }
        var pocName by remember { mutableStateOf("") }
        var pocNumber by remember { mutableStateOf("") }
        var areaCode by remember { mutableStateOf("") }
        var remarks by remember { mutableStateOf("") }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Install Donation Box", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { focusManager.clearFocus() })
                    }
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Register a new donation box location. Move the map to refine the location.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )

                LocationSelectionComponent(
                    selectedLocation = selectedLocation,
                    onLocationChanged = { viewModel.updateLocation(it) },
                    onMyLocationClick = { viewModel.requestCurrentLocation() }
                )

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
                        if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                            pocNumber = it
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
                    label = { Text("Area Code / Sector") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.installBox(address, pocName, pocNumber, areaCode, remarks)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isSubmitting && address.isNotBlank() && pocName.isNotBlank() && pocNumber.length == 10 && areaCode.isNotBlank()
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Confirm Installation", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
