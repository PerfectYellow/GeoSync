package com.example.geosync.admin

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.example.geosync.network.StoredLocation
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

data class MapCameraState(
    val latitude: Double,
    val longitude: Double,
    val zoom: Double
)

@Composable
actual fun GoogleMapView(
    modifier: Modifier,
    locations: Map<String, StoredLocation>,
    mapMode: MapMode,
    selectedClientId: String?,
    focusTrigger: Long,
    defaultLatitude: Double?,
    defaultLongitude: Double?
) {
    // Hoist camera state to sync between engines
    var cameraState by remember { 
        mutableStateOf(MapCameraState(defaultLatitude ?: 35.6994, defaultLongitude ?: 51.3377, 14.0)) 
    }

    Box(modifier = modifier) {
        if (mapMode == MapMode.MAP_IR) {
            MapLibreMapView(
                modifier = Modifier,
                locations = locations,
                selectedClientId = selectedClientId,
                focusTrigger = focusTrigger,
                cameraState = cameraState,
                onCameraChanged = { cameraState = it }
            )
        } else {
            OsmdroidMapView(
                modifier = Modifier,
                locations = locations,
                mapMode = mapMode,
                selectedClientId = selectedClientId,
                focusTrigger = focusTrigger,
                defaultLatitude = defaultLatitude,
                defaultLongitude = defaultLongitude,
                cameraState = cameraState,
                onCameraChanged = { cameraState = it }
            )
        }
    }
}

@Composable
private fun MapLibreMapView(
    modifier: Modifier,
    locations: Map<String, StoredLocation>,
    selectedClientId: String?,
    focusTrigger: Long,
    cameraState: MapCameraState,
    onCameraChanged: (MapCameraState) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val labelBgColor = MaterialTheme.colorScheme.primary.toArgb()
    
    val mapView = remember {
        org.maplibre.android.maps.MapView(context).apply {
            getMapAsync { map ->
                map.setStyle("https://map.ir/vector/styles/main/mapir-xyz-style.json") {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        LatLng(cameraState.latitude, cameraState.longitude), 
                        cameraState.zoom
                    ))
                }
                map.uiSettings.isAttributionEnabled = false
                map.uiSettings.isLogoEnabled = false
                map.uiSettings.isCompassEnabled = true
                map.uiSettings.isRotateGesturesEnabled = true
                map.uiSettings.isTiltGesturesEnabled = true

                map.addOnCameraIdleListener {
                    val pos = map.cameraPosition
                    pos.target?.let { target ->
                        onCameraChanged(MapCameraState(target.latitude, target.longitude, pos.zoom))
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
    var centeredClientIds by remember { mutableStateOf(setOf<String>()) }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            view.getMapAsync { map ->
                map.clear()
                locations.forEach { (id, location) ->
                    val pos = LatLng(location.latitude, location.longitude)
                    val shortId = if (id.length > 10) "${id.take(4)}...${id.takeLast(4)}" else id
                    val iconBitmap = createTextBitmap(context, shortId, labelBgColor)
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
    defaultLatitude: Double?,
    defaultLongitude: Double?,
    cameraState: MapCameraState,
    onCameraChanged: (MapCameraState) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val labelBgColor = MaterialTheme.colorScheme.primary.toArgb()
    
    val isInitialized = remember { mutableStateOf(false) }
    LaunchedEffect(context) {
        org.osmdroid.config.Configuration.getInstance().userAgentValue = context.packageName
        org.osmdroid.config.Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE))
        isInitialized.value = true
    }

    val mapFile: File? = remember(context) {
        val file = File(context.cacheDir, "Tehran.map")
        if (!file.exists()) {
            try {
                context.assets.open("map/Tehran.map").use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }
                file
            } catch (e: Exception) { null }
        } else file
    }

    var currentMapMode by remember { mutableStateOf<MapMode?>(null) }
    var centeredClientIds by remember { mutableStateOf(setOf<String>()) }
    var lastFocusTrigger by remember { mutableStateOf(0L) }

    if (isInitialized.value) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                MapView(ctx).apply {
                    setMultiTouchControls(true)
                    setUseDataConnection(true)
                    setBuiltInZoomControls(true)
                    
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
                            if (center != null) {
                                onCameraChanged(MapCameraState(center.latitude, center.longitude, zoomLevelDouble))
                            }
                        }
                    })
                }
            },
            update = { mapView ->
                if (currentMapMode != mapMode) {
                    try { mapView.tileProvider?.detach() } catch (e: Exception) {}

                    try {
                        when (mapMode) {
                            MapMode.OFFLINE -> {
                                if (mapFile != null && mapFile.exists()) {
                                    val forgeSource = MapsForgeTileSource.createFromFiles(arrayOf(mapFile), InternalRenderTheme.DEFAULT, "Offline")
                                    forgeSource.setUserScaleFactor(context.resources.displayMetrics.density)
                                    mapView.setTileProvider(MapsForgeTileProvider(org.osmdroid.tileprovider.util.SimpleRegisterReceiver(context), forgeSource, null))
                                    mapView.setTileSource(forgeSource)
                                    mapView.setUseDataConnection(false)
                                    if (defaultLatitude != null && defaultLongitude != null) {
                                        mapView.controller.setCenter(GeoPoint(defaultLatitude, defaultLongitude))
                                        mapView.controller.setZoom(14.0)
                                    }
                                }
                            }
                            MapMode.MAP_IR -> {} // Swapped in GoogleMapView
                            else -> {
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
                    val marker = Marker(mapView).apply {
                        position = point
                        icon = createTextDrawable(context, shortId, labelBgColor)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
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
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 34f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    val bounds = Rect()
    paint.getTextBounds(text, 0, text.length, bounds)
    val paddingH = 24f
    val paddingV = 16f
    val tailSize = 15f
    val width = bounds.width() + paddingH * 2
    val height = bounds.height() + paddingV * 2 + tailSize
    val bitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val rectF = RectF(0f, 0f, width, height - tailSize)
    canvas.drawRoundRect(rectF, 12f, 12f, backgroundPaint)
    val path = android.graphics.Path()
    path.moveTo(width / 2f - tailSize, height - tailSize)
    path.lineTo(width / 2f + tailSize, height - tailSize)
    path.lineTo(width / 2f, height)
    path.close()
    canvas.drawPath(path, backgroundPaint)
    canvas.drawText(text, width / 2f, (height - tailSize) / 2f - bounds.centerY(), paint)
    return bitmap
}

private fun createTextDrawable(context: android.content.Context, text: String, bgColor: Int): BitmapDrawable {
    return BitmapDrawable(context.resources, createTextBitmap(context, text, bgColor))
}
