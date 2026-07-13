package com.olivetrust.charity.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
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
    onPinClick: (Location) -> Unit,
    showMyLocationButton: Boolean,
    pins: List<Location>,
    isSelectorMode: Boolean
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

            override fun mapView(mapView: MKMapView, didSelectAnnotationView: MKAnnotationView) {
                val annotation = didSelectAnnotationView.annotation ?: return
                annotation.coordinate.useContents {
                    onPinClick(Location(latitude, longitude))
                }
                // Deselect to allow re-selection
                mapView.deselectAnnotation(annotation, true)
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

    LaunchedEffect(pins) {
        mapView.removeAnnotations(mapView.annotations)
        pins.forEach { pin ->
            val annotation = MKPointAnnotation()
            annotation.setCoordinate(CLLocationCoordinate2DMake(pin.latitude, pin.longitude))
            mapView.addAnnotation(annotation)
        }
    }

    Box(modifier = modifier) {
        UIKitView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { }
        )

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
