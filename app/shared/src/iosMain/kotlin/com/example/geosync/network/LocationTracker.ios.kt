package com.example.geosync.network

class IosLocationTracker : LocationTracker {
    override fun startTracking(trackingId: String) {
        // iOS implementation would use CLLocationManager with background modes
    }
    override fun stopTracking() {
    }
}

actual fun getPlatformTracker(): LocationTracker = IosLocationTracker()
