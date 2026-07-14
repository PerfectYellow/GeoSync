package com.example.geosync.network

interface LocationTracker {
    fun startTracking(trackingId: String)
    fun stopTracking()
}

expect fun getPlatformTracker(): LocationTracker
