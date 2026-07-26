package com.example.geosync.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.geosync.network.StoredLocation

@Composable
actual fun GoogleMapView(
    modifier: Modifier,
    locations: Map<String, StoredLocation>,
    mapMode: MapMode,
    selectedClientId: String?,
    focusTrigger: Long,
    defaultLatitude: Double?,
    defaultLongitude: Double?,
    cameraState: MapCameraState,
    onCameraChanged: (MapCameraState) -> Unit,
    reviewSession: com.example.geosync.network.TrackingSessionHistory?
) {
    Box(modifier = modifier.background(Color.DarkGray)) {
        Text("Map not supported on JS yet", color = Color.White, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
actual fun HistoryMapView(
    modifier: Modifier,
    points: List<com.example.geosync.network.HistoryPoint>,
    routeColor: Color
) {
    Box(modifier = modifier.background(Color.Gray))
}

@Composable
actual fun HistoryReviewMapView(
    modifier: Modifier,
    session: com.example.geosync.network.TrackingSessionHistory,
    cameraState: MapCameraState,
    onCameraChanged: (MapCameraState) -> Unit
) {
    Box(modifier = modifier.background(Color.DarkGray))
}
