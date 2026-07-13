package com.olivetrust.charity.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.olivetrust.charity.Location
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.*
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun InteractiveMap(
    modifier: Modifier,
    initialLocation: Location?,
    onCameraIdle: (Location) -> Unit,
    showMyLocationButton: Boolean
) {
    val mapView = remember { MKMapView() }
    
    val delegate = remember {
        object : NSObject(), MKMapViewDelegateProtocol {
            override fun mapView(mapView: MKMapView, regionDidChangeAnimated: Boolean) {
                val center = mapView.centerCoordinate
                center.useContents {
                    onCameraIdle(Location(latitude, longitude))
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        mapView.delegate = delegate
        if (showMyLocationButton) {
            mapView.showsUserLocation = true
        }
    }

    LaunchedEffect(initialLocation) {
        initialLocation?.let {
            val coordinate = CLLocationCoordinate2DMake(it.latitude, it.longitude)
            val region = MKCoordinateRegionMakeWithDistance(coordinate, 1000.0, 1000.0)
            mapView.setRegion(region, animated = true)
        }
    }

    UIKitView(
        factory = { mapView },
        modifier = modifier.fillMaxSize(),
        update = { }
    )
}
