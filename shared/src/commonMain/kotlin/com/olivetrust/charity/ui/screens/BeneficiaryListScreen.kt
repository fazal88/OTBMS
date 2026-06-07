package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

class BeneficiaryListScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<BeneficiaryListViewModel>()
        val beneficiaries by viewModel.beneficiaries.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Beneficiaries") })
            }
        ) { padding ->
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(beneficiaries) { beneficiary ->
                    ListItem(
                        headlineContent = { Text(beneficiary.headName) },
                        supportingContent = { Text(beneficiary.phoneNumber) },
                        trailingContent = {
                            Row {
                                TextButton(onClick = { navigator.push(AidDistributionScreen(beneficiary.id, beneficiary.headName)) }) {
                                    Text("Aid")
                                }
                                TextButton(onClick = { navigator.push(VerificationVisitScreen(beneficiary.id, beneficiary.headName)) }) {
                                    Text("Visit")
                                }
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
