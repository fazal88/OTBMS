package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.olivetrust.charity.domain.model.Beneficiary
import com.olivetrust.charity.ui.previews.PreviewMocks

class BeneficiaryListScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<BeneficiaryListViewModel>()
        val beneficiaries by viewModel.beneficiaries.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        BeneficiaryListContent(
            beneficiaries = beneficiaries,
            onBeneficiaryClick = { id -> navigator.push(BeneficiaryDetailScreen(id)) },
            onAidClick = { id, name -> navigator.push(AidDistributionScreen(id, name)) },
            onVisitClick = { id, name -> navigator.push(VerificationVisitScreen(id, name)) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeneficiaryListContent(
    beneficiaries: List<Beneficiary>,
    onBeneficiaryClick: (String) -> Unit,
    onAidClick: (String, String) -> Unit,
    onVisitClick: (String, String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Beneficiaries") })
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(beneficiaries) { beneficiary ->
                ListItem(
                    modifier = Modifier.clickable {
                        onBeneficiaryClick(beneficiary.id)
                    },
                    headlineContent = { Text(beneficiary.headName) },
                    supportingContent = { Text(beneficiary.phoneNumber) },
                    trailingContent = {
                        Row {
                            TextButton(onClick = { onAidClick(beneficiary.id, beneficiary.headName) }) {
                                Text("Aid")
                            }
                            TextButton(onClick = { onVisitClick(beneficiary.id, beneficiary.headName) }) {
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

@Preview
@Composable
fun BeneficiaryListContentPreview() {
    MaterialTheme {
        BeneficiaryListContent(
            beneficiaries = listOf(
                PreviewMocks.mockBeneficiary,
                PreviewMocks.mockBeneficiary.copy(id = "b2", headName = "John Doe", phoneNumber = "9876543210")
            ),
            onBeneficiaryClick = {},
            onAidClick = { _, _ -> },
            onVisitClick = { _, _ -> }
        )
    }
}
