package com.example.geosync.network

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings

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

actual fun getPlatformTracker(): LocationTracker {
    val context = _androidContext
    return if (context == null) {
        object : LocationTracker {
            override fun startTracking(trackingId: String) {}
            override fun stopTracking() {}
        }
    } else {
        AndroidLocationTracker(context)
    }
}

actual fun isIgnoringBatteryOptimizations(): Boolean {
    val context = _androidContext ?: return true
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

actual fun openBatteryOptimizationSettings() {
    val context = _androidContext ?: return
    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
