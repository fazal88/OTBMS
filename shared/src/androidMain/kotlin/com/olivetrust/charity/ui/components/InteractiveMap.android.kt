package com.olivetrust.charity.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.olivetrust.charity.Location

@Composable
actual fun InteractiveMap(
    modifier: Modifier,
    initialLocation: Location?,
    onCameraIdle: (Location) -> Unit,
    showMyLocationButton: Boolean
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

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(
            myLocationButtonEnabled = showMyLocationButton,
            zoomControlsEnabled = false,
            mapToolbarEnabled = false
        ),
        properties = MapProperties(
            isMyLocationEnabled = showMyLocationButton
        )
    )
}
