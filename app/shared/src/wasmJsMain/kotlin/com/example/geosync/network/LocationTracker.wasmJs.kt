package com.example.geosync.network

class WasmLocationTracker : LocationTracker {
    override fun startTracking(trackingId: String) {}
    override fun stopTracking() {}
}

actual fun getPlatformTracker(): LocationTracker = WasmLocationTracker()
