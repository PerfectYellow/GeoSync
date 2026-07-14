package com.example.geosync.admin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.geosync.network.StoredLocation

@Composable
expect fun GoogleMapView(
    modifier: Modifier,
    locations: Map<String, StoredLocation>
)
