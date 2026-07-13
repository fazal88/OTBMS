package com.olivetrust.charity.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.olivetrust.charity.Location

@Composable
actual fun InteractiveMap(
    modifier: Modifier,
    initialLocation: Location?,
    onCameraIdle: (Location) -> Unit,
    onPinClick: (Location) -> Unit,
    showMyLocationButton: Boolean,
    pins: List<Location>,
    isSelectorMode: Boolean
) {
    val cameraPositionState = rememberCameraPositionState {
        initialLocation?.let {
            position = CameraPosition.fromLatLngZoom(LatLng(it.latitude, it.longitude), 15f)
        }
    }

    LaunchedEffect(initialLocation) {
        initialLocation?.let {
            cameraPositionState.animate(
                com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                    LatLng(it.latitude, it.longitude),
                    15f
                )
            )
        }
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val target = cameraPositionState.position.target
            onCameraIdle(Location(target.latitude, target.longitude))
        }
    }

    Box(modifier = modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = showMyLocationButton,
                zoomControlsEnabled = false,
                mapToolbarEnabled = false
            ),
            properties = MapProperties(
                isMyLocationEnabled = showMyLocationButton
            )
        ) {
            pins.forEach { pin ->
                val markerState = rememberMarkerState(position = LatLng(pin.latitude, pin.longitude))
                Marker(
                    state = markerState,
                    title = "Location",
                    onClick = {
                        onPinClick(pin)
                        true
                    }
                )
            }
        }

        if (isSelectorMode) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Select Location",
                tint = Color.Red,
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.Center)
            )
        }
    }
}
