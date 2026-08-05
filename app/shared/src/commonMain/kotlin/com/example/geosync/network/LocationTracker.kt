package com.example.geosync.network

interface LocationTracker {
    fun startTracking(trackingId: String)
    fun stopTracking()
    fun manualUpdate()
}

expect fun getPlatformTracker(): LocationTracker

expect fun isIgnoringBatteryOptimizations(): Boolean
expect fun openBatteryOptimizationSettings()
