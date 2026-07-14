package com.example.geosync.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.Service
import android.content.Intent
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
import kotlin.random.Random

class TrackingService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var trackingJob: Job? = null
    private val CHANNEL_ID = "tracking_channel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_TRACKING" -> {
                val trackingId = intent.getStringExtra("TRACKING_ID") ?: return START_NOT_STICKY
                startForeground(1, createNotification("Tracking active: $trackingId"))
                startBroadcasting(trackingId)
            }
            "STOP_TRACKING" -> {
                stopBroadcasting()
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
                geoHttpClient.webSocket(
                    host = ApiConfig.HOST,
                    port = ApiConfig.PORT,
                    path = ApiConfig.WS_LIVE_PATH
                ) {
                    TrackingStatus.updateStatus(ConnectionStatus.CONNECTED)
                    GeoNotificationManager.show("Connected to relay", NotificationType.SUCCESS)
                    sendSerialized(LiveLocationMessage(type = "client.register", clientId = id))
                    
                    while (isActive) {
                        val lat = 37.7749 + (Random.nextDouble() - 0.5) * 0.01
                        val lon = -122.4194 + (Random.nextDouble() - 0.5) * 0.01
                        
                        sendSerialized(LiveLocationMessage(
                            type = "client.location",
                            clientId = id,
                            latitude = lat,
                            longitude = lon,
                            timestamp = Clock.System.now().toString()
                        ))
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
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GeoSync Tracking")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
