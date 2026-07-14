package com.example.geosync.network

import android.content.Context
import android.content.Intent

class AndroidLocationTracker(private val context: Context) : LocationTracker {
    override fun startTracking(trackingId: String) {
        val intent = Intent(context, TrackingService::class.java).apply {
            action = "START_TRACKING"
            putExtra("TRACKING_ID", trackingId)
        }
        context.startForegroundService(intent)
    }

    override fun stopTracking() {
        val intent = Intent(context, TrackingService::class.java).apply {
            action = "STOP_TRACKING"
        }
        context.stopService(intent)
    }
}

private var _androidContext: Context? = null
var androidContext: Context
    get() = _androidContext ?: throw IllegalStateException("Android context not initialized")
    set(value) { _androidContext = value }

actual fun getPlatformTracker(): LocationTracker = AndroidLocationTracker(androidContext)
