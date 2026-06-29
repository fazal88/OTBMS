package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.olivetrust.charity.domain.model.IssueType
import org.koin.core.parameter.parametersOf

class ReportIssueScreen(private val boxId: String) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<ReportIssueViewModel> { parametersOf(boxId) }
        val isSubmitting by viewModel.isSubmitting.collectAsState()
        val error by viewModel.error.collectAsState()
        val success by viewModel.success.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(success) {
            if (success) {
                navigator.pop()
            }
        }

        var selectedType by remember { mutableStateOf(IssueType.DAMAGED) }
        var description by remember { mutableStateOf("") }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Report Box Issue") },
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
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Report an issue with Box $boxId. Your current location will be captured.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text("Issue Type", fontWeight = FontWeight.Bold)
                IssueType.entries.forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type }
                        )
                        Text(type.name.replace("_", " ").lowercase().replaceFirstChar { it.titlecase() })
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = {
                        viewModel.reportIssue(selectedType, description)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSubmitting && description.isNotBlank()
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Submit Report")
                    }
                }
            }
        }
    }
}
