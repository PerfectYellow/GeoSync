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
    selectedClientId: String?,
    focusTrigger: Long
) {
    Box(modifier = modifier.background(Color.DarkGray)) {
        Text("Map not supported on Desktop yet", color = Color.White, modifier = Modifier.align(Alignment.Center))
    }
}
