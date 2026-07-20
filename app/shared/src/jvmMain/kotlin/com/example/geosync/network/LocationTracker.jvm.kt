package com.example.geosync.network

class JvmLocationTracker : LocationTracker {
    override fun startTracking(trackingId: String) {}
    override fun stopTracking() {}
}

actual fun getPlatformTracker(): LocationTracker = JvmLocationTracker()

actual fun isIgnoringBatteryOptimizations(): Boolean = true
actual fun openBatteryOptimizationSettings() {}
