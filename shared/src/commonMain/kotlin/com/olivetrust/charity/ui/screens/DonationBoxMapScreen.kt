package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.olivetrust.charity.domain.model.DonationBox
import com.olivetrust.charity.domain.model.DonationBoxStatus
import com.olivetrust.charity.openMaps

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
                // Placeholder for actual Map
                Column(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Text("Interactive Map Placeholder", color = MaterialTheme.colorScheme.outline)
                    Text("In a real app, this would be Google/Apple Maps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    
                    Button(onClick = { 
                        if (boxes.isNotEmpty()) {
                            val first = boxes.first()
                            openMaps(first.latitude, first.longitude, "Donation Boxes")
                        }
                    }, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Open in External Map")
                    }
                }

                // Simulated Markers
                boxes.forEachIndexed { index, box ->
                    // Randomly place markers for visual effect in this placeholder
                    val x = (index * 70 % 300).dp
                    val y = (index * 110 % 500).dp
                    
                    BoxMarker(
                        modifier = Modifier.offset(x = x, y = y),
                        status = box.status,
                        onClick = { selectedBox = box }
                    )
                }

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

@Composable
fun BoxMarker(modifier: Modifier, status: DonationBoxStatus, onClick: () -> Unit) {
    val color = when (status) {
        DonationBoxStatus.APPROVED_ACTIVE -> Color(0xFF4CAF50)
        DonationBoxStatus.PENDING_APPROVAL -> Color(0xFFFF9800)
        DonationBoxStatus.REJECTED -> Color(0xFFF44336)
        DonationBoxStatus.OUT_OF_ORDER -> Color(0xFFFF5722)
        DonationBoxStatus.DECOMMISSIONED -> Color(0xFF607D8B)
    }
    
    IconButton(onClick = onClick, modifier = modifier.background(color, CircleShape)) {
        Icon(Icons.Default.LocationOn, null, tint = Color.White)
    }
}
