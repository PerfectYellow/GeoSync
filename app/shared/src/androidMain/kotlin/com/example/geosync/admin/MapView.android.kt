package com.example.geosync.admin

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.zIndex
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.geosync.network.StoredLocation
import com.example.geosync.network.ApiConfig
import org.mapsforge.map.rendertheme.InternalRenderTheme
import org.osmdroid.mapsforge.MapsForgeTileProvider
import org.osmdroid.mapsforge.MapsForgeTileSource
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.Polyline
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapLibreMapOptions
import java.io.File
import java.io.FileOutputStream

/**
 * Single source of truth for Offline Map configuration.
 * Change [FILE_BASE_NAME] or [PREFERRED_FORMAT] here to update the entire app's offline behavior.
 */
object OfflineMapConfig {
    const val FILE_BASE_NAME = "Tehran"
    const val ASSET_FOLDER = "map"
    const val STYLE_JSON_NAME = "style.json"
    const val OSM_STYLE_NAME = "osm_raster_style.json"

    val PREFERRED_FORMAT = MapFormat.MAPFORGE

    /**
     * Scale factor for Mapsforge (.map). 
     * Lower values (e.g., 0.3f - 0.5f) make text and lines smaller.
     */
    const val MAPFORGE_SCALE_MODIFIER = 0.35f
    
    /**
     * Scale factor for client markers.
     * Lower values make the bubble markers smaller.
     */
    const val MARKER_SCALE_MODIFIER = 0.5f

    /**
     * Theme for Mapsforge. OSMARENDER is detailed, DEFAULT is basic.
     */
    val MAPFORGE_THEME = InternalRenderTheme.OSMARENDER

    enum class MapFormat {
        PMTILES, MAPFORGE
    }

    // Derived paths
    val pmtilesFileName get() = "$FILE_BASE_NAME.pmtiles"
    val mapforgeFileName get() = "$FILE_BASE_NAME.map"
    
    val pmtilesAssetPath get() = "$ASSET_FOLDER/$pmtilesFileName"
    val mapforgeAssetPath get() = "$ASSET_FOLDER/$mapforgeFileName"
    val styleAssetPath get() = "$ASSET_FOLDER/$STYLE_JSON_NAME"
    val osmStyleAssetPath get() = "$ASSET_FOLDER/$OSM_STYLE_NAME"
}


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
    // Trigger for programmatic zoom/position changes
    var externalMoveTrigger by remember { mutableLongStateOf(0L) }
    var isMapReady by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // RESET isMapReady when switching between Live and Review modes to prevent black flash
    LaunchedEffect(reviewSession != null) {
        isMapReady = false
    }
    
    // Determine which engine to use for offline mode based on config and file availability
    val useMapLibreForOffline = remember(context, mapMode) { 
        if (mapMode != MapMode.OFFLINE) return@remember false
        
        val pmtilesInCache = File(context.cacheDir, OfflineMapConfig.pmtilesFileName).exists()
        val pmtilesInAssets = try { 
            context.assets.list(OfflineMapConfig.ASSET_FOLDER)?.contains(OfflineMapConfig.pmtilesFileName) == true 
        } catch(e: Exception) { false }
        
        val pmtilesAvailable = pmtilesInCache || pmtilesInAssets
        
        // If PMTILES is preferred and available, use MapLibre
        if (OfflineMapConfig.PREFERRED_FORMAT == OfflineMapConfig.MapFormat.PMTILES && pmtilesAvailable) {
            true
        } else if (OfflineMapConfig.PREFERRED_FORMAT == OfflineMapConfig.MapFormat.MAPFORGE) {
            // If MAPFORGE is preferred, we only use MapLibre if .map is missing but .pmtiles is there
            val mapInCache = File(context.cacheDir, OfflineMapConfig.mapforgeFileName).exists()
            val mapInAssets = try { 
                context.assets.list(OfflineMapConfig.ASSET_FOLDER)?.contains(OfflineMapConfig.mapforgeFileName) == true 
            } catch(e: Exception) { false }
            
            !mapInCache && !mapInAssets && pmtilesAvailable
        } else {
            pmtilesAvailable
        }
    }

    Box(modifier = modifier) {
        if (mapMode == MapMode.MAP_IR || mapMode == MapMode.INTERNAL || mapMode == MapMode.OPEN_STREET || useMapLibreForOffline) {
            MapLibreMapView(
                modifier = Modifier.fillMaxSize(),
                locations = if (reviewSession != null) emptyMap() else locations,
                mapMode = mapMode,
                selectedClientId = selectedClientId,
                focusTrigger = focusTrigger,
                externalMoveTrigger = externalMoveTrigger,
                cameraState = cameraState,
                onCameraChanged = onCameraChanged,
                onMapReady = { isMapReady = true },
                isMapReady = isMapReady,
                reviewSession = reviewSession
            )
        } else {
            OsmdroidMapView(
                modifier = Modifier.fillMaxSize(),
                locations = if (reviewSession != null) emptyMap() else locations,
                mapMode = mapMode,
                selectedClientId = selectedClientId,
                focusTrigger = focusTrigger,
                externalMoveTrigger = externalMoveTrigger,
                defaultLatitude = defaultLatitude,
                defaultLongitude = defaultLongitude,
                cameraState = cameraState,
                onCameraChanged = onCameraChanged,
                onMapReady = { isMapReady = true },
                isMapReady = isMapReady,
                reviewSession = reviewSession
            )
        }

        if (!isMapReady) {
            MapPlaceholder(Modifier.fillMaxSize())
        }

        // Solid background to prevent transparency holes during transitions
        Box(Modifier.fillMaxSize().background(Color.White).zIndex(-1f))
        
        // Zoom Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            FilledIconButton(
                onClick = {
                    onCameraChanged(cameraState.copy(zoom = (cameraState.zoom + 1).coerceAtMost(20.0)))
                    externalMoveTrigger++
                },
                modifier = Modifier.size(44.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In")
            }

            FilledIconButton(
                onClick = {
                    onCameraChanged(cameraState.copy(zoom = (cameraState.zoom - 1).coerceAtLeast(1.0)))
                    externalMoveTrigger++
                },
                modifier = Modifier.size(44.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
            }

            if (locations.isNotEmpty()) {
                FilledIconButton(
                    onClick = {
                        val lats = locations.values.map { it.latitude }
                        val lngs = locations.values.map { it.longitude }
                        val avgLat = lats.average()
                        val avgLng = lngs.average()
                        onCameraChanged(cameraState.copy(latitude = avgLat, longitude = avgLng))
                        externalMoveTrigger++
                    },
                    modifier = Modifier.size(44.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        contentColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Fit to Clients")
                }
            }
        }
    }
}

@Composable
private fun MapLibreMapView(
    modifier: Modifier,
    locations: Map<String, StoredLocation>,
    mapMode: MapMode,
    selectedClientId: String?,
    focusTrigger: Long,
    externalMoveTrigger: Long,
    cameraState: MapCameraState,
    onCameraChanged: (MapCameraState) -> Unit,
    onMapReady: () -> Unit,
    isMapReady: Boolean,
    reviewSession: com.example.geosync.network.TrackingSessionHistory? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val primaryColorArgb = MaterialTheme.colorScheme.primary.toArgb()
    val startColor = Color(0xFF4CAF50).toArgb()
    val endColor = Color(0xFFF44336).toArgb()
    
    // Ensure offline assets are copied
    var assetsReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val styleFile = File(context.cacheDir, OfflineMapConfig.STYLE_JSON_NAME)
            val pmtilesFile = File(context.cacheDir, OfflineMapConfig.pmtilesFileName)
            
            if (!pmtilesFile.exists()) {
                try {
                    context.assets.open(OfflineMapConfig.pmtilesAssetPath).use { input ->
                        FileOutputStream(pmtilesFile).use { output -> input.copyTo(output) }
                    }
                } catch (e: Exception) {
                    Log.e("MapView", "Failed to copy PMTiles", e)
                }
            }

            if (!styleFile.exists()) {
                try {
                    context.assets.open(OfflineMapConfig.styleAssetPath).use { input ->
                        val content = input.bufferedReader().use { it.readText() }
                        val fixedContent = content.replace("{PMTILES_PATH}", pmtilesFile.absolutePath)
                        FileOutputStream(styleFile).use { output -> 
                            output.writer().use { it.write(fixedContent) }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MapView", "Failed to copy style.json", e)
                }
            }

            // Also ensure OSM style is available in cache
            val osmStyleFile = File(context.cacheDir, OfflineMapConfig.OSM_STYLE_NAME)
            if (!osmStyleFile.exists()) {
                try {
                    context.assets.open(OfflineMapConfig.osmStyleAssetPath).use { input ->
                        FileOutputStream(osmStyleFile).use { output -> input.copyTo(output) }
                    }
                } catch (e: Exception) {}
            }
            assetsReady = true
        }
    }

    val styleUrl = when {
        mapMode == MapMode.OPEN_STREET -> {
            val osmStyleFile = File(context.cacheDir, OfflineMapConfig.OSM_STYLE_NAME)
            if (assetsReady && osmStyleFile.exists() && osmStyleFile.length() > 0) {
                "file://${osmStyleFile.absolutePath}"
            } else {
                // Fallback to online OSM style if local asset fails
                "https://raw.githubusercontent.com/maplibre/demotiles/gh-pages/style.json"
            }
        }
        mapMode == MapMode.INTERNAL -> {
            val protocol = if (ApiConfig.isSecure) "https" else "http"
            "$protocol://${ApiConfig.HOST}:${ApiConfig.PORT}/v1/map/style.json"
        }
        mapMode == MapMode.OFFLINE -> {
            val styleFile = File(context.cacheDir, OfflineMapConfig.STYLE_JSON_NAME)
            if (assetsReady && styleFile.exists() && styleFile.length() > 0) {
                "file://${styleFile.absolutePath}"
            } else {
                "https://map.ir/vector/styles/main/mapir-xyz-style.json"
            }
        }
        else -> "https://map.ir/vector/styles/main/mapir-xyz-style.json"
    }

    val mapView = remember {
        val options = MapLibreMapOptions.createFromAttributes(context, null)
            .localIdeographFontFamily("sans-serif")
            .textureMode(true) // Texture Mode is safer for Compose layering

        org.maplibre.android.maps.MapView(context, options).apply {
            setBackgroundColor(android.graphics.Color.WHITE)
            
            getMapAsync { map ->
                map.uiSettings.isAttributionEnabled = false
                map.uiSettings.isLogoEnabled = false
                map.uiSettings.isCompassEnabled = true

                map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    LatLng(cameraState.latitude, cameraState.longitude),
                    cameraState.zoom
                ))

                map.addOnCameraIdleListener {
                    val pos = map.cameraPosition
                    pos.target?.let { target ->
                        if (target.latitude != 0.0 || target.longitude != 0.0 || pos.zoom > 1.0) {
                            onCameraChanged(MapCameraState(target.latitude, target.longitude, pos.zoom))
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(mapView) {
        mapView.onStart()
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    var lastExternalMoveTrigger by remember { mutableLongStateOf(0L) }
    var currentStyleUrl by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var lastFocusTrigger by remember { mutableLongStateOf(0L) }

    // Opaque background layer
    Box(modifier = modifier.background(Color.White)) {
        // Dedicated Drawing and Focus Logic
        LaunchedEffect(mapView, locations, reviewSession, currentStyleUrl, isMapReady, selectedClientId, focusTrigger) {
            if (isMapReady) {
                mapView.getMapAsync { map ->
                    try {
                        map.clear()
                        
                        if (reviewSession != null) {
                            val points = reviewSession.points
                            if (points.isNotEmpty()) {
                                val latLngs = points.map { LatLng(it.latitude, it.longitude) }
                                
                                // 1. Draw path (Road)
                                if (latLngs.size > 1) {
                                    map.addPolyline(org.maplibre.android.annotations.PolylineOptions()
                                        .addAll(latLngs)
                                        .color(primaryColorArgb)
                                        .width(8f))
                                }
                                
                                // 2. Start marker
                                val startPos = latLngs.first()
                                val startIcon = IconFactory.getInstance(context).fromBitmap(
                                    createTextBitmap(context, "START", startColor, tailAtTop = false)
                                )
                                map.addMarker(MarkerOptions().position(startPos).icon(startIcon))
                                
                                // 3. End marker
                                if (latLngs.size >= 2) {
                                    val endPos = latLngs.last()
                                    val endIcon = IconFactory.getInstance(context).fromBitmap(
                                        createTextBitmap(context, "END", endColor, tailAtTop = true)
                                    )
                                    map.addMarker(MarkerOptions().position(endPos).icon(endIcon))
                                }
                                
                                // 4. Zoom
                                if (latLngs.size > 1) {
                                    val boundsBuilder = org.maplibre.android.geometry.LatLngBounds.Builder()
                                    latLngs.forEach { boundsBuilder.include(it) }
                                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120))
                                } else {
                                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(startPos, 16.0))
                                }
                            }
                        } else {
                            var focusAttempted = false
                            locations.forEach { (id, location) ->
                                val pos = LatLng(location.latitude, location.longitude)
                                val shortId = if (id.length > 10) "${id.take(4)}...${id.takeLast(4)}" else id
                                val clientColor = AdminUtils.getClientColor(id).toArgb()
                                val iconBitmap = createTextBitmap(context, shortId, clientColor)
                                val icon = IconFactory.getInstance(context).fromBitmap(iconBitmap)
                                
                                map.addMarker(MarkerOptions().position(pos).icon(icon).title(shortId))

                                if (id == selectedClientId && focusTrigger != lastFocusTrigger) {
                                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15.0))
                                    focusAttempted = true
                                }
                            }

                            if (focusAttempted || locations.isEmpty() || selectedClientId == null) {
                                lastFocusTrigger = focusTrigger
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MapLibre", "Drawing error", e)
                    }
                }
            }
        }

        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.getMapAsync { map ->
                    if (currentStyleUrl != styleUrl) {
                        map.setStyle(styleUrl) {
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                LatLng(cameraState.latitude, cameraState.longitude),
                                cameraState.zoom
                            ))
                            scope.launch {
                                kotlinx.coroutines.delay(300)
                                onMapReady()
                            }
                        }
                        currentStyleUrl = styleUrl
                    } else if (!isMapReady) {
                        onMapReady()
                    }

                    if (externalMoveTrigger != lastExternalMoveTrigger) {
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            LatLng(cameraState.latitude, cameraState.longitude),
                            cameraState.zoom
                        ))
                        lastExternalMoveTrigger = externalMoveTrigger
                    }
                    view.invalidate()
                }
            }
        )
    }
}

@Composable
private fun OsmdroidMapView(
    modifier: Modifier,
    locations: Map<String, StoredLocation>,
    mapMode: MapMode,
    selectedClientId: String?,
    focusTrigger: Long,
    externalMoveTrigger: Long,
    defaultLatitude: Double?,
    defaultLongitude: Double?,
    cameraState: MapCameraState,
    onCameraChanged: (MapCameraState) -> Unit,
    onMapReady: () -> Unit,
    isMapReady: Boolean,
    reviewSession: com.example.geosync.network.TrackingSessionHistory? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val primaryColorArgb = MaterialTheme.colorScheme.primary.toArgb()
    val startColor = Color(0xFF4CAF50).toArgb()
    val endColor = Color(0xFFF44336).toArgb()
    
    val isInitialized = remember { mutableStateOf(false) }
    LaunchedEffect(context) {
        org.osmdroid.config.Configuration.getInstance().userAgentValue = context.packageName
        isInitialized.value = true
        kotlinx.coroutines.delay(1000)
        onMapReady()
    }

    val mapFile: File? = remember(context) {
        val file = File(context.cacheDir, OfflineMapConfig.mapforgeFileName)
        if (!file.exists()) {
            try {
                context.assets.open(OfflineMapConfig.mapforgeAssetPath).use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }
                file
            } catch (e: Exception) { null }
        } else file
    }

    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            setUseDataConnection(true)
            setBuiltInZoomControls(false)
            setBackgroundColor(android.graphics.Color.WHITE)
            
            // Texture Mode is better for lists/sheets
            @Suppress("DEPRECATION")
            isDrawingCacheEnabled = true
            
            overlays.add(RotationGestureOverlay(this))
            val compassOverlay = CompassOverlay(context, InternalCompassOrientationProvider(context), this)
            compassOverlay.enableCompass()
            overlays.add(compassOverlay)

            controller.setZoom(cameraState.zoom)
            controller.setCenter(GeoPoint(cameraState.latitude, cameraState.longitude))

            addMapListener(object : org.osmdroid.events.MapListener {
                override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                    updateSharedCamera()
                    return true
                }
                override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean {
                    updateSharedCamera()
                    return true
                }
                private fun updateSharedCamera() {
                    val center = mapCenter as? GeoPoint
                    if (center != null && (center.latitude != 0.0 || center.longitude != 0.0 || zoomLevelDouble > 1.0)) {
                        onCameraChanged(MapCameraState(center.latitude, center.longitude, zoomLevelDouble))
                    }
                }
            })
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
        }
    }

    var currentMapMode by remember { mutableStateOf<MapMode?>(null) }
    var centeredClientIds by remember { mutableStateOf(setOf<String>()) }
    var lastFocusTrigger by remember { mutableStateOf(0L) }
    var lastExternalMoveTrigger by remember { mutableStateOf(0L) }

    // Dedicated Drawing Logic for Live/Review modes
    LaunchedEffect(mapView, locations, reviewSession, isInitialized.value, isMapReady, centeredClientIds, selectedClientId, focusTrigger) {
        if (isMapReady && isInitialized.value) {
            val geoPoints = if (reviewSession != null) {
                reviewSession.points.map { GeoPoint(it.latitude, it.longitude) }
            } else emptyList()

            mapView.overlays.clear()
            
            if (reviewSession != null) {
                if (geoPoints.isNotEmpty()) {
                    // 1. Draw path (Road)
                    if (geoPoints.size > 1) {
                        val line = Polyline().apply {
                            setPoints(geoPoints)
                            @Suppress("DEPRECATION")
                            color = primaryColorArgb
                            @Suppress("DEPRECATION")
                            width = 12f
                        }
                        mapView.overlays.add(line)
                    }
                    
                    // 2. Start Marker
                    val startMarker = Marker(mapView).apply {
                        position = geoPoints.first()
                        icon = createTextDrawable(context, "START", startColor, tailAtTop = false)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    mapView.overlays.add(startMarker)
                    
                    // 3. End Marker
                    if (geoPoints.size >= 2) {
                        val endMarker = Marker(mapView).apply {
                            position = geoPoints.last()
                            icon = createTextDrawable(context, "END", endColor, tailAtTop = true)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP)
                        }
                        mapView.overlays.add(endMarker)
                    }
                    
                    // 4. Zoom
                    if (geoPoints.size > 1) {
                        val bounds = org.osmdroid.util.BoundingBox.fromGeoPoints(geoPoints)
                        mapView.zoomToBoundingBox(bounds, true, 120)
                    } else {
                        mapView.controller.setCenter(geoPoints.first())
                        mapView.controller.setZoom(16.0)
                    }
                }
            } else {
                locations.forEach { (id, location) ->
                    val point = GeoPoint(location.latitude, location.longitude)
                    val shortId = if (id.length > 10) "${id.take(4)}...${id.takeLast(4)}" else id
                    val clientColor = AdminUtils.getClientColor(id).toArgb()
                    val marker = Marker(mapView).apply {
                        position = point
                        icon = createTextDrawable(context, shortId, clientColor)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = shortId
                    }
                    mapView.overlays.add(marker)

                    if (id !in centeredClientIds) {
                        mapView.controller.animateTo(point)
                        // Note: in a real app, update state via callback or shared state
                    }
                    if (id == selectedClientId && focusTrigger != lastFocusTrigger) {
                        mapView.controller.animateTo(point)
                    }
                }
            }
            mapView.invalidate()
        }
    }

    if (isInitialized.value) {
        // Force onMapReady when reset
        LaunchedEffect(isInitialized.value, isMapReady) {
            if (isInitialized.value && !isMapReady) {
                onMapReady()
            }
        }
        
        AndroidView(
            modifier = modifier,
            factory = { mapView },
            update = { view ->
                if (externalMoveTrigger != lastExternalMoveTrigger) {
                    view.controller.animateTo(
                        GeoPoint(cameraState.latitude, cameraState.longitude),
                        cameraState.zoom,
                        1000L
                    )
                    lastExternalMoveTrigger = externalMoveTrigger
                }

                if (currentMapMode != mapMode) {
                    try { view.tileProvider?.detach() } catch (e: Exception) {}
                    try {
                        when (mapMode) {
                            MapMode.OFFLINE -> {
                                if (mapFile != null && mapFile.exists()) {
                                    val forgeSource = MapsForgeTileSource.createFromFiles(
                                        arrayOf(mapFile), 
                                        OfflineMapConfig.MAPFORGE_THEME, 
                                        "Offline"
                                    )
                                    forgeSource.setUserScaleFactor(context.resources.displayMetrics.density * OfflineMapConfig.MAPFORGE_SCALE_MODIFIER)
                                    view.setTileProvider(MapsForgeTileProvider(org.osmdroid.tileprovider.util.SimpleRegisterReceiver(context), forgeSource, null))
                                    view.setTileSource(forgeSource)
                                    view.setUseDataConnection(false)
                                }
                            }
                            else -> {
                                view.setTileProvider(MapTileProviderBasic(context, TileSourceFactory.MAPNIK))
                                view.setTileSource(TileSourceFactory.MAPNIK)
                                view.setUseDataConnection(true)
                            }
                        }
                        currentMapMode = mapMode
                    } catch (e: Exception) {
                        currentMapMode = MapMode.OPEN_STREET
                    }
                }
                
                if (focusTrigger != lastFocusTrigger) lastFocusTrigger = focusTrigger
                view.invalidate()
            }
        )
    }
}

@Composable
actual fun HistoryReviewMapView(
    modifier: Modifier,
    session: com.example.geosync.network.TrackingSessionHistory,
    cameraState: MapCameraState,
    onCameraChanged: (MapCameraState) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val primaryColorArgb = MaterialTheme.colorScheme.primary.toArgb()
    val clientColor = AdminUtils.getClientColor(session.clientId ?: "")
    
    val startColor = 0xFF4CAF50.toInt()
    val endColor = 0xFFF44336.toInt()

    val styleUrl = "asset://${OfflineMapConfig.osmStyleAssetPath}"
    var isStyleReady by remember { mutableStateOf(false) }
    var externalMoveTrigger by remember { mutableLongStateOf(0L) }

    val mapView = remember {
        val options = MapLibreMapOptions.createFromAttributes(context, null)
            .localIdeographFontFamily("sans-serif")
        
        org.maplibre.android.maps.MapView(context, options).apply {
            setBackgroundColor(android.graphics.Color.WHITE)
            getMapAsync { map ->
                map.uiSettings.isAttributionEnabled = false
                map.uiSettings.isLogoEnabled = false
                
                map.setStyle(styleUrl) {
                    isStyleReady = true
                }

                map.addOnCameraIdleListener {
                    val pos = map.cameraPosition
                    pos.target?.let { target ->
                        if (target.latitude != 0.0 || target.longitude != 0.0 || pos.zoom > 1.0) {
                            onCameraChanged(MapCameraState(target.latitude, target.longitude, pos.zoom))
                        }
                    }
                }
            }
        }
    }

    // Initial Auto-Focus logic
    var initialFocusDone by remember { mutableStateOf(false) }
    LaunchedEffect(mapView, session, isStyleReady) {
        if (isStyleReady && !initialFocusDone) {
            mapView.getMapAsync { map ->
                map.clear()
                val points = session.points
                if (points.isNotEmpty()) {
                    val latLngs = points.map { LatLng(it.latitude, it.longitude) }
                    
                    // 1. Path (Neon Glow) - Only draw if there's significant movement
                    if (latLngs.size > 2 || session.totalDistanceKm > 0.005) {
                        map.addPolyline(org.maplibre.android.annotations.PolylineOptions()
                            .addAll(latLngs)
                            .color(android.graphics.Color.argb(60, android.graphics.Color.red(clientColor.toArgb()), android.graphics.Color.green(clientColor.toArgb()), android.graphics.Color.blue(clientColor.toArgb())))
                            .width(18f))
                        
                        map.addPolyline(org.maplibre.android.annotations.PolylineOptions()
                            .addAll(latLngs)
                            .color(clientColor.toArgb())
                            .width(8f))
                    }

                    // 2. Start Marker
                    val startIcon = IconFactory.getInstance(context).fromBitmap(
                        createModernMarker(context, true, startColor).bitmap
                    )
                    map.addMarker(MarkerOptions().position(latLngs.first()).icon(startIcon))
                    
                    // 3. End Marker
                    if (latLngs.size >= 2) {
                        val endIcon = IconFactory.getInstance(context).fromBitmap(
                            createModernMarker(context, false, endColor).bitmap
                        )
                        map.addMarker(MarkerOptions().position(latLngs.last()).icon(endIcon))
                    }
                    
                    // 4. Initial Perfect Zoom
                    if (latLngs.size > 2 && session.totalDistanceKm > 0.005) {
                        val boundsBuilder = org.maplibre.android.geometry.LatLngBounds.Builder()
                        latLngs.forEach { boundsBuilder.include(it) }
                        map.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100))
                    } else {
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngs.first(), 17.0))
                    }
                    initialFocusDone = true
                }
            }
        }
    }

    DisposableEffect(mapView) {
        mapView.onStart()
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    Box(modifier = modifier.background(Color.White)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView },
            update = { view ->
                view.getMapAsync { map ->
                    if (externalMoveTrigger > 0) {
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            LatLng(cameraState.latitude, cameraState.longitude),
                            cameraState.zoom
                        ))
                    }
                }
            }
        )

        // Zoom Controls (Brought back for history map)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            FilledIconButton(
                onClick = {
                    onCameraChanged(cameraState.copy(zoom = (cameraState.zoom + 1).coerceAtMost(20.0)))
                    externalMoveTrigger++
                },
                modifier = Modifier.size(44.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In")
            }

            FilledIconButton(
                onClick = {
                    onCameraChanged(cameraState.copy(zoom = (cameraState.zoom - 1).coerceAtLeast(1.0)))
                    externalMoveTrigger++
                },
                modifier = Modifier.size(44.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
            }
            
            // Re-focus on trip button
            FilledIconButton(
                onClick = {
                    initialFocusDone = false // Trigger the LaunchedEffect logic again
                },
                modifier = Modifier.size(44.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Center on Trip")
            }
        }

        if (!isStyleReady) {
            MapPlaceholder(Modifier.fillMaxSize())
        }
    }
}

private fun createModernMarker(
    context: android.content.Context,
    isStart: Boolean,
    color: Int
): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val size = (if (isStart) 14f else 22f) * density
    val width = size.toInt().coerceAtLeast(1)
    val height = (size * 1.2f).toInt().coerceAtLeast(1)
    
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    if (isStart) {
        // Simple elegant circle for start
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.color = color
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 3f * density, paint)
    } else {
        // Modern pin shape for end
        val path = android.graphics.Path()
        val radius = size / 2f
        val centerX = size / 2f
        val centerY = size / 2f
        
        // Pin head
        path.addCircle(centerX, centerY, radius, android.graphics.Path.Direction.CW)
        
        // Pin tip
        val tipPath = android.graphics.Path()
        tipPath.moveTo(centerX - radius * 0.8f, centerY + radius * 0.5f)
        tipPath.lineTo(centerX, size * 1.15f)
        tipPath.lineTo(centerX + radius * 0.8f, centerY + radius * 0.5f)
        tipPath.close()
        
        path.op(tipPath, android.graphics.Path.Op.UNION)
        
        // Draw shadow
        paint.color = android.graphics.Color.BLACK
        paint.alpha = 40
        canvas.drawCircle(centerX, size * 1.15f, 4f * density, paint)
        
        // Draw pin
        paint.color = color
        paint.alpha = 255
        canvas.drawPath(path, paint)
        
        // Inner white circle
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(centerX, centerY, radius * 0.4f, paint)
    }

    return BitmapDrawable(context.resources, bitmap)
}

private fun createTextBitmap(
    context: android.content.Context, 
    text: String, 
    bgColor: Int, 
    tailAtTop: Boolean = false
): Bitmap {
    val density = context.resources.displayMetrics.density
    val scale = density * OfflineMapConfig.MARKER_SCALE_MODIFIER
    
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 14f * scale
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    val bounds = Rect()
    paint.getTextBounds(text, 0, text.length, bounds)
    
    val paddingH = 12f * scale
    val paddingV = 10f * scale
    val tailSize = 10f * scale
    val cornerRadius = 12f * scale
    
    val widthFloat = (bounds.width() + paddingH * 2).coerceAtLeast(40f * scale).coerceAtLeast(1f)
    val heightFloat = (bounds.height() + paddingV * 2 + tailSize).coerceAtLeast(40f * scale).coerceAtLeast(1f)
    
    val width = kotlin.math.ceil(widthFloat).toInt().coerceAtLeast(1)
    val height = kotlin.math.ceil(heightFloat).toInt().coerceAtLeast(1)
    
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    val path = android.graphics.Path()
    if (tailAtTop) {
        // Bubble is BELOW the location (Tail points UP, pointer at TOP)
        val rectF = RectF(0f, tailSize, widthFloat, heightFloat)
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, backgroundPaint)
        
        path.moveTo(widthFloat / 2f - tailSize, tailSize)
        path.lineTo(widthFloat / 2f, 0f)
        path.lineTo(widthFloat / 2f + tailSize, tailSize)
        path.close()
        canvas.drawPath(path, backgroundPaint)
        
        canvas.drawText(text, widthFloat / 2f, (heightFloat + tailSize) / 2f - bounds.centerY(), paint)
    } else {
        // Bubble is ABOVE the location (Tail points DOWN, pointer at BOTTOM)
        val rectF = RectF(0f, 0f, widthFloat, heightFloat - tailSize)
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, backgroundPaint)
        
        path.moveTo(widthFloat / 2f - tailSize, heightFloat - tailSize)
        path.lineTo(widthFloat / 2f, heightFloat)
        path.lineTo(widthFloat / 2f + tailSize, heightFloat - tailSize)
        path.close()
        canvas.drawPath(path, backgroundPaint)
        
        canvas.drawText(text, widthFloat / 2f, (heightFloat - tailSize) / 2f - bounds.centerY(), paint)
    }
    return bitmap
}

private fun createTextDrawable(
    context: android.content.Context, 
    text: String, 
    bgColor: Int, 
    tailAtTop: Boolean = false
): BitmapDrawable {
    return BitmapDrawable(context.resources, createTextBitmap(context, text, bgColor, tailAtTop))
}

@Composable
fun MapPlaceholder(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "MapLoading")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulsatingAlpha"
    )

    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerTranslate"
    )

    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface) // Solid fallback background
            .background(surfaceColor)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.05f),
                        primaryColor.copy(alpha = 0.15f * alpha),
                        primaryColor.copy(alpha = 0.05f)
                    ),
                    start = Offset(shimmerTranslate - 500f, shimmerTranslate - 500f),
                    end = Offset(shimmerTranslate, shimmerTranslate)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.MyLocation,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = primaryColor.copy(alpha = 0.2f * alpha)
        )
    }
}

@Composable
actual fun HistoryMapView(
    modifier: Modifier,
    points: List<com.example.geosync.network.HistoryPoint>,
    routeColor: Color
) {
    val primaryColorArgb = MaterialTheme.colorScheme.primary.toArgb()
    val actualRouteColor = if (routeColor != Color.Unspecified) routeColor.toArgb() else primaryColorArgb
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val startColor = 0xFF4CAF50.toInt()
    val endColor = 0xFFF44336.toInt()

    if (points.isEmpty()) {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Route, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
        }
        return
    }

    val isInitialized = remember { mutableStateOf(false) }
    
    // Config OSMLoader for compliance
    LaunchedEffect(context) {
        org.osmdroid.config.Configuration.getInstance().apply {
            userAgentValue = "GeoSync/1.0 (ir.icodes.geosync; contact@icodes.ir)"
            additionalHttpRequestProperties["Referer"] = "android://ir.icodes.geosync"
            tileDownloadThreads = 2
        }
        isInitialized.value = true
    }

    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(false)
            setUseDataConnection(true)
            setBuiltInZoomControls(false)
            setBackgroundColor(android.graphics.Color.WHITE)
            setHasTransientState(true)
            
            // Use basic Mapnik (OSM)
            setTileProvider(MapTileProviderBasic(context, TileSourceFactory.MAPNIK))
            setTileSource(TileSourceFactory.MAPNIK)
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
        }
    }

    // Handle drawing logic in a stable way
    LaunchedEffect(mapView, points, actualRouteColor, isInitialized.value) {
        if (isInitialized.value) {
            val geoPoints = points.map { GeoPoint(it.latitude, it.longitude) }
            if (geoPoints.isNotEmpty()) {
                mapView.overlays.clear()
                
                // 1. Dual Layer Glow Path
                val glowLine = Polyline().apply {
                    setPoints(geoPoints)
                    @Suppress("DEPRECATION")
                    color = android.graphics.Color.argb(60, android.graphics.Color.red(actualRouteColor), android.graphics.Color.green(actualRouteColor), android.graphics.Color.blue(actualRouteColor))
                    @Suppress("DEPRECATION")
                    width = 22f
                }
                mapView.overlays.add(glowLine)

                val line = Polyline().apply {
                    setPoints(geoPoints)
                    @Suppress("DEPRECATION")
                    color = actualRouteColor
                    @Suppress("DEPRECATION")
                    width = 10f
                }
                mapView.overlays.add(line)

                // 2. Modern Markers
                val startMarker = Marker(mapView).apply {
                    position = geoPoints.first()
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = createModernMarker(context, true, startColor)
                }
                mapView.overlays.add(startMarker)

                if (geoPoints.size >= 2) {
                    val endMarker = Marker(mapView).apply {
                        position = geoPoints.last()
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = createModernMarker(context, false, endColor)
                    }
                    mapView.overlays.add(endMarker)
                }

                // 3. Perfect Zoom
                if (geoPoints.size > 1) {
                    val bounds = org.osmdroid.util.BoundingBox.fromGeoPoints(geoPoints)
                    mapView.zoomToBoundingBox(bounds, false, 80)
                } else {
                    mapView.controller.setCenter(geoPoints.first())
                    mapView.controller.setZoom(16.0)
                }
                mapView.invalidate()
            }
        }
    }

    Box(modifier = modifier.background(Color.White)) {
        if (isInitialized.value) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { mapView },
                update = { /* Updates are handled in LaunchedEffect */ }
            )
        } else {
            MapPlaceholder(Modifier.fillMaxSize())
        }
    }
}
