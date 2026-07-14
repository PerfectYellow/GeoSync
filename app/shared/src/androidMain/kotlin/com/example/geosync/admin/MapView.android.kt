package com.example.geosync.admin

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.example.geosync.network.StoredLocation
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
actual fun GoogleMapView(
    modifier: Modifier,
    locations: Map<String, StoredLocation>
) {
    // OpenStreetMap needs a user agent to download tiles
    val context = androidx.compose.ui.platform.LocalContext.current
    val labelBgColor = MaterialTheme.colorScheme.primary.toArgb()
    
    org.osmdroid.config.Configuration.getInstance().userAgentValue = context.packageName

    // Track which locations we've already centered on to avoid jumping
    var centeredClientIds by remember { mutableStateOf(setOf<String>()) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(15.0)
            }
        },
        update = { mapView ->
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
            }
            mapView.invalidate() // Refresh map
        }
    )
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
