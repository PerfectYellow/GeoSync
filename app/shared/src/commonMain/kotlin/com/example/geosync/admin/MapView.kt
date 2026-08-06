package com.example.geosync.admin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.geosync.network.StoredLocation

@Composable
expect fun GoogleMapView(
    modifier: Modifier,
    locations: Map<String, StoredLocation>,
    mapMode: MapMode,
    selectedClientId: String? = null,
    focusTrigger: Long = 0L,
    defaultLatitude: Double? = null,
    defaultLongitude: Double? = null,
    cameraState: MapCameraState,
    onCameraChanged: (MapCameraState) -> Unit,
    reviewSession: com.example.geosync.network.TrackingSessionHistory? = null
)

@Composable
expect fun HistoryMapView(
    modifier: Modifier,
    points: List<com.example.geosync.network.HistoryPoint>,
    routeColor: Color = Color.Unspecified
)

@Composable
expect fun HistoryReviewMapView(
    modifier: Modifier,
    session: com.example.geosync.network.TrackingSessionHistory,
    cameraState: MapCameraState,
    focusTrigger: Long = 0L,
    onCameraChanged: (MapCameraState) -> Unit,
    onMapInteraction: () -> Unit,
    onScannedPointChange: (Int?) -> Unit = {}
)
