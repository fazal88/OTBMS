package com.olivetrust.charity.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.olivetrust.charity.Location

@Composable
expect fun InteractiveMap(
    modifier: Modifier = Modifier,
    initialLocation: Location?,
    onCameraIdle: (Location) -> Unit = {},
    onPinClick: (Location) -> Unit = {},
    showMyLocationButton: Boolean = true,
    pins: List<Location> = emptyList(),
    isSelectorMode: Boolean = false
)
