package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.olivetrust.charity.Location
import com.olivetrust.charity.domain.model.DonationBox
import com.olivetrust.charity.ui.components.InteractiveMap

data class DonationBoxMapScreen(private val filters: DonationBoxFilters) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<DonationBoxListViewModel>()
        val boxes by viewModel.boxes.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        
        LaunchedEffect(filters) {
            viewModel.updateFilters(filters)
        }
        
        var selectedBox by remember { mutableStateOf<DonationBox?>(null) }
        val initialLocation = remember(boxes) {
            boxes.firstOrNull()?.let { Location(it.latitude, it.longitude) }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Map View") },
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
            Box(Modifier.padding(padding).fillMaxSize()) {
                InteractiveMap(
                    modifier = Modifier.fillMaxSize(),
                    initialLocation = initialLocation,
                    pins = boxes.map { Location(it.latitude, it.longitude) },
                    onPinClick = { clickedLocation ->
                        selectedBox = boxes.find { 
                            it.latitude == clickedLocation.latitude && it.longitude == clickedLocation.longitude 
                        }
                    }
                )

                // Bottom Sheet / Card for selected box
                if (selectedBox != null) {
                    val box = selectedBox!!
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { navigator.push(DonationBoxDetailScreen(box.id)) },
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(box.id, style = MaterialTheme.typography.labelSmall)
                                DonationBoxStatusBadge(box.status)
                            }
                            Text(box.address, fontWeight = FontWeight.Bold)
                            Text("${box.personOfContact} • ${box.areaCode}", style = MaterialTheme.typography.bodySmall)
                            
                            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Last Collection", style = MaterialTheme.typography.labelSmall)
                                    Text(box.lastCollectionDate?.let { "₹ ${box.lastCollectedAmount}" } ?: "Never", fontWeight = FontWeight.SemiBold)
                                }
                                Button(onClick = { selectedBox = null }) {
                                    Text("Close")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
