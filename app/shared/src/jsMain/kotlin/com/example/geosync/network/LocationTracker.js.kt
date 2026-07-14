package com.example.geosync.network

class JsLocationTracker : LocationTracker {
    override fun startTracking(trackingId: String) {}
    override fun stopTracking() {}
}

actual fun getPlatformTracker(): LocationTracker = JsLocationTracker()
