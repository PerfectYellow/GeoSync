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
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
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
    onCameraChanged: (MapCameraState) -> Unit
) {
    // Trigger for programmatic zoom/position changes
    var externalMoveTrigger by remember { mutableLongStateOf(0L) }
    var isMapReady by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
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
                locations = locations,
                mapMode = mapMode,
                selectedClientId = selectedClientId,
                focusTrigger = focusTrigger,
                externalMoveTrigger = externalMoveTrigger,
                cameraState = cameraState,
                onCameraChanged = onCameraChanged,
                onMapReady = { isMapReady = true }
            )
        } else {
            OsmdroidMapView(
                modifier = Modifier.fillMaxSize(),
                locations = locations,
                mapMode = mapMode,
                selectedClientId = selectedClientId,
                focusTrigger = focusTrigger,
                externalMoveTrigger = externalMoveTrigger,
                defaultLatitude = defaultLatitude,
                defaultLongitude = defaultLongitude,
                cameraState = cameraState,
                onCameraChanged = onCameraChanged,
                onMapReady = { isMapReady = true }
            )
        }

        if (!isMapReady) {
            MapPlaceholder(Modifier.fillMaxSize())
        }

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
    onMapReady: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
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
            if (assetsReady && osmStyleFile.exists()) {
                "file://${osmStyleFile.absolutePath}"
            } else {
                // Fallback to internal if file not ready, though MapLibre allows asset:// directly
                "asset://${OfflineMapConfig.osmStyleAssetPath}"
            }
        }
        mapMode == MapMode.INTERNAL -> {
            val protocol = if (ApiConfig.isSecure) "https" else "http"
            "$protocol://${ApiConfig.HOST}:${ApiConfig.PORT}/v1/map/style.json"
        }
        mapMode == MapMode.OFFLINE -> {
            val styleFile = File(context.cacheDir, OfflineMapConfig.STYLE_JSON_NAME)
            if (assetsReady && styleFile.exists()) {
                "file://${styleFile.absolutePath}"
            } else {
                // Fallback while copying or if failed
                "https://map.ir/vector/styles/main/mapir-xyz-style.json"
            }
        }
        else -> "https://map.ir/vector/styles/main/mapir-xyz-style.json"
    }

    val mapView = remember {
        org.maplibre.android.maps.MapView(context).apply {
            getMapAsync { map ->
                map.uiSettings.isAttributionEnabled = false
                map.uiSettings.isLogoEnabled = false
                map.uiSettings.isCompassEnabled = true
                map.uiSettings.isRotateGesturesEnabled = true
                map.uiSettings.isTiltGesturesEnabled = true

                // Initialize camera IMMEDIATELY to avoid global map flash
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    LatLng(cameraState.latitude, cameraState.longitude),
                    cameraState.zoom
                ))

                map.addOnCameraIdleListener {
                    val pos = map.cameraPosition
                    pos.target?.let { target ->
                        // Only report if it's NOT the uninitialized default state
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

    var lastFocusTrigger by remember { mutableStateOf(0L) }
    var lastExternalMoveTrigger by remember { mutableStateOf(0L) }
    var centeredClientIds by remember { mutableStateOf(setOf<String>()) }
    var currentStyleUrl by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            view.getMapAsync { map ->
                // Apply style if changed
                if (currentStyleUrl != styleUrl) {
                    map.setStyle(styleUrl) {
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            LatLng(cameraState.latitude, cameraState.longitude),
                            cameraState.zoom
                        ))
                        // Add a small extra delay to ensure the surface has fully swapped
                        // from the black background to the first rendered tiles.
                        scope.launch {
                            kotlinx.coroutines.delay(300)
                            onMapReady()
                        }
                    }
                    currentStyleUrl = styleUrl
                }

                // Apply external move (buttons)
                if (externalMoveTrigger != lastExternalMoveTrigger) {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        LatLng(cameraState.latitude, cameraState.longitude),
                        cameraState.zoom
                    ))
                    lastExternalMoveTrigger = externalMoveTrigger
                }

                map.clear()
                locations.forEach { (id, location) ->
                    val pos = LatLng(location.latitude, location.longitude)
                    val shortId = if (id.length > 10) "${id.take(4)}...${id.takeLast(4)}" else id
                    val clientColor = AdminUtils.getClientColor(id).toArgb()
                    val iconBitmap = createTextBitmap(context, shortId, clientColor)
                    val icon = IconFactory.getInstance(context).fromBitmap(iconBitmap)
                    
                    map.addMarker(MarkerOptions().position(pos).icon(icon).title(shortId))

                    // Auto-center on a client the first time we receive their location
                    if (id !in centeredClientIds) {
                        map.animateCamera(CameraUpdateFactory.newLatLng(pos))
                        centeredClientIds = centeredClientIds + id
                    }

                    if (id == selectedClientId && focusTrigger != lastFocusTrigger) {
                        map.animateCamera(CameraUpdateFactory.newLatLng(pos))
                    }
                }
                if (focusTrigger != lastFocusTrigger) {
                    lastFocusTrigger = focusTrigger
                }
            }
        }
    )
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
    onMapReady: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val isInitialized = remember { mutableStateOf(false) }
    LaunchedEffect(context) {
        // Redundant setting here as a safety measure for tile downloader
        org.osmdroid.config.Configuration.getInstance().userAgentValue = context.packageName
        isInitialized.value = true
        // Increased delay for Osmdroid to finish its first draw to avoid black flicker
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

    var currentMapMode by remember { mutableStateOf<MapMode?>(null) }
    var centeredClientIds by remember { mutableStateOf(setOf<String>()) }
    var lastFocusTrigger by remember { mutableStateOf(0L) }
    var lastExternalMoveTrigger by remember { mutableStateOf(0L) }

    if (isInitialized.value) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                MapView(ctx).apply {
                    setMultiTouchControls(true)
                    setUseDataConnection(true)
                    setBuiltInZoomControls(false) // Using custom buttons
                    
                    overlays.add(RotationGestureOverlay(this))
                    val compassOverlay = CompassOverlay(ctx, InternalCompassOrientationProvider(ctx), this)
                    compassOverlay.enableCompass()
                    overlays.add(compassOverlay)

                    val scaleBarOverlay = ScaleBarOverlay(this)
                    scaleBarOverlay.setCentred(true)
                    scaleBarOverlay.setScaleBarOffset(ctx.resources.displayMetrics.widthPixels / 2, 10)
                    overlays.add(scaleBarOverlay)

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
            },
            update = { mapView ->
                // Apply external move (buttons)
                if (externalMoveTrigger != lastExternalMoveTrigger) {
                    mapView.controller.animateTo(
                        GeoPoint(cameraState.latitude, cameraState.longitude),
                        cameraState.zoom,
                        1000L
                    )
                    lastExternalMoveTrigger = externalMoveTrigger
                }

                if (currentMapMode != mapMode) {
                    try { mapView.tileProvider?.detach() } catch (e: Exception) {}

                    try {
                        when (mapMode) {
                            MapMode.OFFLINE -> {
                                if (mapFile != null && mapFile.exists()) {
                                    val forgeSource = MapsForgeTileSource.createFromFiles(
                                        arrayOf(mapFile), 
                                        OfflineMapConfig.MAPFORGE_THEME, 
                                        "Offline"
                                    )
                                    val density = context.resources.displayMetrics.density
                                    // Using a smaller modifier to reduce the "huge" text effect
                                    forgeSource.setUserScaleFactor(density * OfflineMapConfig.MAPFORGE_SCALE_MODIFIER)
                                    mapView.setTileProvider(MapsForgeTileProvider(org.osmdroid.tileprovider.util.SimpleRegisterReceiver(context), forgeSource, null))
                                    mapView.setTileSource(forgeSource)
                                    mapView.setUseDataConnection(false)
                                    if (defaultLatitude != null && defaultLongitude != null) {
                                        mapView.controller.setCenter(GeoPoint(defaultLatitude, defaultLongitude))
                                        mapView.controller.setZoom(14.0)
                                    }
                                }
                            }
                            MapMode.MAP_IR, MapMode.INTERNAL, MapMode.OPEN_STREET -> {
                                mapView.setTileProvider(MapTileProviderBasic(context, TileSourceFactory.MAPNIK))
                                mapView.setTileSource(TileSourceFactory.MAPNIK)
                                mapView.setUseDataConnection(true)
                                mapView.controller.setCenter(GeoPoint(cameraState.latitude, cameraState.longitude))
                                mapView.controller.setZoom(cameraState.zoom)
                            }
                        }
                        currentMapMode = mapMode
                        mapView.invalidate()
                    } catch (e: Exception) {
                        mapView.setTileProvider(MapTileProviderBasic(context, TileSourceFactory.MAPNIK))
                        mapView.setTileSource(TileSourceFactory.MAPNIK)
                        currentMapMode = MapMode.OPEN_STREET
                        mapView.invalidate()
                    }
                }

                mapView.overlays.removeAll { it is Marker }
                locations.forEach { (id, location) ->
                    val point = GeoPoint(location.latitude, location.longitude)
                    val shortId = if (id.length > 10) "${id.take(4)}...${id.takeLast(4)}" else id
                    val clientColor = AdminUtils.getClientColor(id).toArgb()
                    val marker = Marker(mapView).apply {
                        position = point
                        icon = createTextDrawable(context, shortId, clientColor)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM) // Tail points to coordinate
                        title = shortId
                    }
                    mapView.overlays.add(marker)

                    if (id !in centeredClientIds) {
                        mapView.controller.animateTo(point)
                        centeredClientIds = centeredClientIds + id
                    }
                    if (id == selectedClientId && focusTrigger != lastFocusTrigger) {
                        mapView.controller.animateTo(point)
                    }
                }
                if (focusTrigger != lastFocusTrigger) lastFocusTrigger = focusTrigger
                mapView.invalidate()
            }
        )
    }
}

private fun createTextBitmap(context: android.content.Context, text: String, bgColor: Int): Bitmap {
    val density = context.resources.displayMetrics.density
    val scale = density * OfflineMapConfig.MARKER_SCALE_MODIFIER
    
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 12f * scale
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    val bounds = Rect()
    paint.getTextBounds(text, 0, text.length, bounds)
    
    val paddingH = 8f * scale
    val paddingV = 6f * scale
    val tailSize = 6f * scale
    val cornerRadius = 6f * scale
    
    val widthFloat = (bounds.width() + paddingH * 2).coerceAtLeast(1f)
    val heightFloat = (bounds.height() + paddingV * 2 + tailSize).coerceAtLeast(1f)
    
    val width = kotlin.math.ceil(widthFloat).toInt()
    val height = kotlin.math.ceil(heightFloat).toInt()
    
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    val rectF = RectF(0f, 0f, widthFloat, heightFloat - tailSize)
    canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, backgroundPaint)
    
    val path = android.graphics.Path()
    path.moveTo(widthFloat / 2f - tailSize, heightFloat - tailSize)
    path.lineTo(widthFloat / 2f + tailSize, heightFloat - tailSize)
    path.lineTo(widthFloat / 2f, heightFloat)
    path.close()
    canvas.drawPath(path, backgroundPaint)
    
    canvas.drawText(text, widthFloat / 2f, (heightFloat - tailSize) / 2f - bounds.centerY(), paint)
    return bitmap
}

private fun createTextDrawable(context: android.content.Context, text: String, bgColor: Int): BitmapDrawable {
    return BitmapDrawable(context.resources, createTextBitmap(context, text, bgColor))
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
