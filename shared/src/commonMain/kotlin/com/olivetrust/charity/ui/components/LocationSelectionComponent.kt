package com.olivetrust.charity.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.olivetrust.charity.Location

@Composable
fun LocationSelectionComponent(
    modifier: Modifier = Modifier,
    selectedLocation: Location?,
    onLocationChanged: (Location) -> Unit,
    onMyLocationClick: () -> Unit
) {
    var isFullScreen by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            MapWithOverlay(
                location = selectedLocation,
                onLocationChanged = onLocationChanged,
                onMyLocationClick = onMyLocationClick,
                onExpandClick = { isFullScreen = true }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        selectedLocation?.let {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Selected Location", style = MaterialTheme.typography.labelSmall)
                        Text(
                            "Lat: ${it.latitude.toString().take(10)}, Lng: ${it.longitude.toString().take(10)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (isFullScreen) {
        FullScreenMapPicker(
            initialLocation = selectedLocation,
            onConfirm = { 
                onLocationChanged(it)
                isFullScreen = false
            },
            onDismiss = { isFullScreen = false },
            onMyLocationClick = onMyLocationClick
        )
    }
}

@Composable
private fun MapWithOverlay(
    location: Location?,
    onLocationChanged: (Location) -> Unit,
    onMyLocationClick: () -> Unit,
    onExpandClick: (() -> Unit)? = null,
    showConfirmButton: Boolean = false,
    onConfirm: ((Location) -> Unit)? = null
) {
    var currentCenter by remember(location) { mutableStateOf(location) }

    Box(modifier = Modifier.fillMaxSize()) {
        InteractiveMap(
            modifier = Modifier.fillMaxSize(),
            initialLocation = location,
            onCameraIdle = {
                currentCenter = it
                onLocationChanged(it)
            },
            showMyLocationButton = false
        )

        // Fixed Center Pin
        Box(
            modifier = Modifier.align(Alignment.Center).padding(bottom = 32.dp)
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Color.Red
            )
        }

        // Overlay Controls
        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            onExpandClick?.let {
                SmallFloatingActionButton(
                    onClick = it,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Search, "Full Screen")
                }
            }

            SmallFloatingActionButton(
                onClick = onMyLocationClick,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Refresh, "My Location")
            }
        }

        if (showConfirmButton && onConfirm != null) {
            Button(
                onClick = { currentCenter?.let { onConfirm(it) } },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirm Location", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FullScreenMapPicker(
    initialLocation: Location?,
    onConfirm: (Location) -> Unit,
    onDismiss: () -> Unit,
    onMyLocationClick: () -> Unit
) {
    var selectedLocation by remember { mutableStateOf(initialLocation) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                CenterAlignedTopAppBar(
                    title = { Text("Select Location", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Close")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                MapWithOverlay(
                    location = initialLocation,
                    onLocationChanged = { selectedLocation = it },
                    onMyLocationClick = onMyLocationClick,
                    showConfirmButton = true,
                    onConfirm = onConfirm
                )
            }
        }
    }
}
