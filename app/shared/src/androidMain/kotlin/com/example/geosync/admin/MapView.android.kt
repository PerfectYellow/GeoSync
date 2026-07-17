package com.example.geosync.admin

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.example.geosync.network.StoredLocation
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.rendertheme.InternalRenderTheme
import org.osmdroid.mapsforge.MapsForgeTileProvider
import org.osmdroid.mapsforge.MapsForgeTileSource
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.io.FileOutputStream

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
    val context = androidx.compose.ui.platform.LocalContext.current
    val labelBgColor = MaterialTheme.colorScheme.primary.toArgb()
    
    // Ensure Osmdroid is initialized
    val isInitialized = remember { mutableStateOf(false) }
    LaunchedEffect(context) {
        org.osmdroid.config.Configuration.getInstance().userAgentValue = context.packageName
        org.osmdroid.config.Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE))
        isInitialized.value = true
    }

    // Copy map file from assets to internal storage if it exists
    val mapFile: File? = remember(context) {
        val file = File(context.cacheDir, "Tehran.map")
        if (!file.exists()) {
            try {
                Log.d("MapView", "Copying Tehran.map from assets...")
                context.assets.open("map/Tehran.map").use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d("MapView", "Copy successful: ${file.absolutePath}")
                file
            } catch (e: Exception) {
                Log.e("MapView", "Error copying map file", e)
                null
            }
        } else {
            Log.d("MapView", "Map file already exists: ${file.absolutePath}")
            file
        }
    }

    // Track current map mode to avoid re-setting provider
    var currentMapMode by remember { mutableStateOf<MapMode?>(null) }

    // Track which locations we've already centered on to avoid jumping
    var centeredClientIds by remember { mutableStateOf(setOf<String>()) }
    
    // Remember the last trigger to detect manual clicks
    var lastFocusTrigger by remember { mutableStateOf(0L) }

    // Remember the last online position
    var lastOnlineCenter by remember { mutableStateOf<GeoPoint?>(null) }

    if (isInitialized.value) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                MapView(ctx).apply {
                    setMultiTouchControls(true)
                    setUseDataConnection(true)
                    controller.setZoom(15.0)
                    
                    // Set initial center if default coordinates are provided
                    if (defaultLatitude != null && defaultLongitude != null) {
                        controller.setCenter(GeoPoint(defaultLatitude, defaultLongitude))
                    }
                }
        },
        update = { mapView ->
            // Only switch provider if mode changed
            if (currentMapMode != mapMode) {
                Log.d("MapView", "Map mode changed: $mapMode")
                
                // Save current position if we were in an online mode and moving to offline
                if (currentMapMode != MapMode.OFFLINE && mapMode == MapMode.OFFLINE) {
                    lastOnlineCenter = mapView.mapCenter as? GeoPoint
                }

                // Detach old provider to release resources
                try {
                    mapView.tileProvider?.detach()
                } catch (e: Exception) {}

                try {
                    when (mapMode) {
                        MapMode.OFFLINE -> {
                            if (mapFile != null && mapFile.exists()) {
                                Log.d("MapView", "Setting Offline Provider")
                                val forgeSource = MapsForgeTileSource.createFromFiles(
                                    arrayOf(mapFile),
                                    InternalRenderTheme.DEFAULT,
                                    "MapsforgeOffline"
                                )
                                forgeSource.setUserScaleFactor(context.resources.displayMetrics.density)
                                
                                val forgeProvider = MapsForgeTileProvider(
                                    org.osmdroid.tileprovider.util.SimpleRegisterReceiver(context),
                                    forgeSource,
                                    null
                                )
                                mapView.setTileProvider(forgeProvider)
                                mapView.setTileSource(forgeSource)
                                mapView.setUseDataConnection(false)
                                
                                if (defaultLatitude != null && defaultLongitude != null) {
                                    mapView.controller.setCenter(GeoPoint(defaultLatitude, defaultLongitude))
                                }
                            }
                        }
                        MapMode.MAP_IR -> {
                            Log.d("MapView", "Setting Map.ir Provider")
                            // placeholder URL for Map.ir
                            val mapIrSource = XYTileSource(
                                "MapIr", 0, 19, 256, ".png",
                                arrayOf("https://map.ir/shiveh/wmts/1.0.0/static/Maps/Vector/GoogleMapsCompatible/")
                            )
                            val provider = MapTileProviderBasic(context, mapIrSource)
                            mapView.setTileProvider(provider)
                            mapView.setTileSource(mapIrSource)
                            mapView.setUseDataConnection(true)
                            restoreOnlineCenter(mapView, lastOnlineCenter, defaultLatitude, defaultLongitude)
                        }
                        MapMode.INTERNAL -> {
                            Log.d("MapView", "Setting Internal Provider")
                            val provider = MapTileProviderBasic(context, TileSourceFactory.MAPNIK)
                            mapView.setTileProvider(provider)
                            mapView.setTileSource(TileSourceFactory.MAPNIK)
                            mapView.setUseDataConnection(true)
                            restoreOnlineCenter(mapView, lastOnlineCenter, defaultLatitude, defaultLongitude)
                        }
                        MapMode.OPEN_STREET -> {
                            Log.d("MapView", "Setting Open Street Provider")
                            val provider = MapTileProviderBasic(context, TileSourceFactory.MAPNIK)
                            mapView.setTileProvider(provider)
                            mapView.setTileSource(TileSourceFactory.MAPNIK)
                            mapView.setUseDataConnection(true)
                            restoreOnlineCenter(mapView, lastOnlineCenter, defaultLatitude, defaultLongitude)
                        }
                    }
                    currentMapMode = mapMode
                    mapView.invalidate()
                } catch (e: Exception) {
                    Log.e("MapView", "Error switching tile provider", e)
                    mapView.setTileProvider(MapTileProviderBasic(context, TileSourceFactory.MAPNIK))
                    mapView.setTileSource(TileSourceFactory.MAPNIK)
                    currentMapMode = MapMode.OPEN_STREET
                    mapView.invalidate()
                }
            }

                // Clear existing markers to prevent duplicates on update
                mapView.overlays.clear()

                locations.forEach { (id, location) ->
                    val point = GeoPoint(location.latitude, location.longitude)
                    val shortId = if (id.length > 10) {
                        "${id.take(4)}...${id.takeLast(4)}"
                    } else {
                        id
                    }
                    
                    // Add Marker with custom text icon
                    val marker = Marker(mapView).apply {
                        position = point
                        icon = createTextDrawable(context, shortId, labelBgColor)
                        // Anchor at bottom center so the "tail" points to the location
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = shortId
                        snippet = "Lat: ${location.latitude}, Lng: ${location.longitude}"
                    }
                    mapView.overlays.add(marker)

                    // Auto-center on a client the first time we receive their location
                    if (id !in centeredClientIds) {
                        mapView.controller.animateTo(point)
                        centeredClientIds = centeredClientIds + id
                    }
                    
                    // If this client was explicitly selected or re-clicked, focus on them
                    if (id == selectedClientId && focusTrigger != lastFocusTrigger) {
                        mapView.controller.animateTo(point)
                        lastFocusTrigger = focusTrigger
                    }
                }
                
                // Sync the trigger state if a different client was selected
                if (focusTrigger != lastFocusTrigger) {
                    lastFocusTrigger = focusTrigger
                }

                mapView.invalidate() // Refresh map
            }
        )
    }
}

private fun restoreOnlineCenter(mapView: MapView, lastOnlineCenter: GeoPoint?, defaultLatitude: Double?, defaultLongitude: Double?) {
    if (lastOnlineCenter != null) {
        mapView.controller.setCenter(lastOnlineCenter)
    } else if (defaultLatitude != null && defaultLongitude != null) {
        mapView.controller.setCenter(GeoPoint(defaultLatitude, defaultLongitude))
    }
}

private fun createTextDrawable(context: android.content.Context, text: String, bgColor: Int): BitmapDrawable {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 34f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
    }

    val bounds = Rect()
    paint.getTextBounds(text, 0, text.length, bounds)

    val paddingH = 24f
    val paddingV = 16f
    val tailSize = 15f
    
    val width = bounds.width() + paddingH * 2
    val height = bounds.height() + paddingV * 2 + tailSize

    val bitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Draw the bubble (rect + tail)
    val rectF = RectF(0f, 0f, width, height - tailSize)
    canvas.drawRoundRect(rectF, 12f, 12f, backgroundPaint)
    
    // Draw triangle tail
    val path = android.graphics.Path()
    path.moveTo(width / 2f - tailSize, height - tailSize)
    path.lineTo(width / 2f + tailSize, height - tailSize)
    path.lineTo(width / 2f, height)
    path.close()
    canvas.drawPath(path, backgroundPaint)
    
    // Draw text
    canvas.drawText(text, width / 2f, (height - tailSize) / 2f - bounds.centerY(), paint)

    return BitmapDrawable(context.resources, bitmap)
}
/*
eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImp0aSI6IjhlZDM2MDA0ZDdkNjEwZjgzZGUxNjE1ZGYxZjY2OWU0OTczMjc2NTA3YjU2Mjk2YzY3ZGRiZTE1ZmI3NDY4ZmNkZjgwOGRhOTRlN2FlYTg1In0.eyJhdWQiOiI0Mjg0OCIsImp0aSI6IjhlZDM2MDA0ZDdkNjEwZjgzZGUxNjE1ZGYxZjY2OWU0OTczMjc2NTA3YjU2Mjk2YzY3ZGRiZTE1ZmI3NDY4ZmNkZjgwOGRhOTRlN2FlYTg1IiwiaWF0IjoxNzg0MzExMzE5LCJuYmYiOjE3ODQzMTEzMTksImV4cCI6MTc4NjkwMzMxOSwic3ViIjoiIiwic2NvcGVzIjpbImJhc2ljIl19.bhCD89v-9X2jid8LPTzMMfXQr_xvz14KoovAZ7n_bIeoNoFB1wrup9xKH9ohEP-27U10KwPGftlqr_ZD9IYd2nN--nwywh21LFHYq8_o5jOueZADG011X4KoZQKLIWQalJvr3DdPnbuBeHBO5G_Xy-siHZmAvlI05fD0qXncl1coY8f-i7bG2sibu5mx2fGYYvLglXPgDfXK4afLwFNGUlYa7kiPF8BzvRzNT_Ila87oZLoOAOhoSayyD3XGk8CZnYLKvqIpnorwWMvvnviZFqYbWW0NCewRc2r7JuCl-k653f2J6pcq54OIhAEtmz0WPzA5jd53CYnKA5d_s4Ur1w
 */