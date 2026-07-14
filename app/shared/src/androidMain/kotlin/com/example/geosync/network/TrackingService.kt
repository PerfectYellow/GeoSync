package com.example.geosync.network

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.geosync.NotificationManager as GeoNotificationManager
import com.example.geosync.NotificationType
import com.example.geosync.network.LiveLocationMessage
import com.example.geosync.network.geoHttpClient
import com.example.geosync.network.ApiConfig
import io.ktor.client.plugins.websocket.*
import kotlinx.coroutines.*
import kotlinx.datetime.Clock

class TrackingService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var trackingJob: Job? = null
    private val CHANNEL_ID = "tracking_channel"
    
    private lateinit var locationManager: LocationManager
    private var lastLocation: Location? = null

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // Logic to choose the best location
            val currentBest = lastLocation
            if (currentBest == null) {
                lastLocation = location
                return
            }

            // Only replace if this one is significantly newer or more accurate
            val isNewer = location.time > currentBest.time
            val isAccurateEnough = location.accuracy < 100 // Less than 100 meters
            val isFromGps = location.provider == LocationManager.GPS_PROVIDER
            
            if (isFromGps || (isNewer && isAccurateEnough)) {
                lastLocation = location
                // Update notification with provider info for debugging
                createNotification("Tracking active: ${location.provider} (${location.accuracy.toInt()}m)")
            }
        }
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String, status: Int, extras: android.os.Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {
            TrackingStatus.updateStatus(ConnectionStatus.FAILED, "GPS Disabled")
            GeoNotificationManager.show("GPS is disabled", NotificationType.ERROR)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_TRACKING" -> {
                val trackingId = intent.getStringExtra("TRACKING_ID") ?: return START_NOT_STICKY
                
                // ALWAYS call startForeground first to avoid "ForegroundServiceDidNotStartInTimeException"
                // On Android 14+ (API 34), we must specify the foreground service type
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        1, 
                        createNotification("GeoSync is initializing..."),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    )
                } else {
                    startForeground(1, createNotification("GeoSync is initializing..."))
                }

                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    GeoNotificationManager.show("GPS is turned off. Please enable it.", NotificationType.ERROR)
                    TrackingStatus.updateStatus(ConnectionStatus.FAILED, "GPS is off")
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return START_NOT_STICKY
                }
                
                try {
                    // Try to get last known location as a starting point, but ONLY if it's fresh (last 5 mins)
                    val fiveMinutesAgo = System.currentTimeMillis() - 5 * 60 * 1000
                    
                    val gpsLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    val netLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    
                    // Prioritize GPS even if slightly older than Network, but both must be "fresh"
                    val bestLastLoc = when {
                        gpsLoc != null && gpsLoc.time > fiveMinutesAgo -> gpsLoc
                        netLoc != null && netLoc.time > fiveMinutesAgo -> netLoc
                        // If no fresh location, at least take the most recent network one to avoid "America" cache
                        netLoc != null -> netLoc 
                        else -> null
                    }
                    
                    lastLocation = bestLastLoc
                    if (bestLastLoc != null) {
                        println("GeoSync: Initialized with ${bestLastLoc.provider} at ${bestLastLoc.latitude}, ${bestLastLoc.longitude}")
                    }

                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        2000L,
                        1f,
                        locationListener
                    )
                    
                    // Also request network updates as a faster (though less accurate) fallback
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        5000L,
                        10f,
                        locationListener
                    )
                    
                    // Update notification text with active ID
                    createNotification("Tracking active: $trackingId")
                } catch (e: SecurityException) {
                    GeoNotificationManager.show("Location permission denied", NotificationType.ERROR)
                    TrackingStatus.updateStatus(ConnectionStatus.FAILED, "Permission denied")
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return START_NOT_STICKY
                }
                
                startBroadcasting(trackingId)
            }
            "STOP_TRACKING" -> {
                stopBroadcasting()
                locationManager.removeUpdates(locationListener)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startBroadcasting(id: String) {
        trackingJob?.cancel()
        trackingJob = serviceScope.launch {
            TrackingStatus.updateStatus(ConnectionStatus.CONNECTING)
            try {
                geoHttpClient.geoLiveWebSocket {
                    TrackingStatus.updateStatus(ConnectionStatus.CONNECTED)
                    GeoNotificationManager.show("Connected to relay", NotificationType.SUCCESS)
                    sendSerialized(LiveLocationMessage(type = "client.register", clientId = id))
                    
                    while (isActive) {
                        val location = lastLocation
                        if (location != null) {
                            println("GeoSync: Sending location from ${location.provider}: ${location.latitude}, ${location.longitude}")
                            sendSerialized(LiveLocationMessage(
                                type = "client.location",
                                clientId = id,
                                latitude = location.latitude,
                                longitude = location.longitude,
                                timestamp = Clock.System.now().toString()
                            ))
                        } else {
                            GeoNotificationManager.show("Waiting for GPS fix...", NotificationType.INFO)
                        }
                        delay(3000)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val errorMsg = "Connection failed: ${e.message}"
                TrackingStatus.updateStatus(ConnectionStatus.FAILED, errorMsg)
                GeoNotificationManager.show(errorMsg, NotificationType.ERROR)
            } finally {
                if (TrackingStatus.status.value == ConnectionStatus.CONNECTED) {
                    TrackingStatus.updateStatus(ConnectionStatus.IDLE)
                }
            }
        }
    }

    private fun stopBroadcasting() {
        trackingJob?.cancel()
        trackingJob = null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Location Tracking",
            android.app.NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(content: String): Notification {
        val manager = getSystemService(android.app.NotificationManager::class.java)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GeoSync Tracking")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
        
        // Update existing notification if it's already showing
        manager.notify(1, notification)
        return notification
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
