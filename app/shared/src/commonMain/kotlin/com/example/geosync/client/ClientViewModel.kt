package com.example.geosync.client

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geosync.NotificationManager
import com.example.geosync.NotificationType
import com.example.geosync.SettingsManager
import com.example.geosync.localization.LocalizationManager
import com.example.geosync.network.*
import io.ktor.client.plugins.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.random.Random
import kotlinx.datetime.Clock

class ClientViewModel(private val isPreview: Boolean = false) : ViewModel() {
    private val tracker = getPlatformTracker()

    val connectionStatus = TrackingStatus.status
    val connectionError = TrackingStatus.errorMessage
    val subscribersCount = TrackingStatus.subscribersCount

    private val _trackingId = MutableStateFlow(SettingsManager.customId ?: generateUuid())
    val trackingId: StateFlow<String> = _trackingId.asStateFlow()

    init {
        if (!isPreview) {
            viewModelScope.launch {
                connectionStatus.collect { status ->
                    if (status == ConnectionStatus.FAILED) {
                        // Auto-hide error after 5 seconds
                        delay(5000)
                        if (TrackingStatus.status.value == ConnectionStatus.FAILED) {
                            TrackingStatus.updateStatus(ConnectionStatus.IDLE)
                        }
                    }
                }
            }
        }
    }

    fun toggleTracking() {
        val status = connectionStatus.value
        if (status == ConnectionStatus.CONNECTED || status == ConnectionStatus.CONNECTING) {
            stopTracking()
        } else {
            startTracking()
        }
    }

    fun refreshTrackingId() {
        if (connectionStatus.value == ConnectionStatus.CONNECTED) {
            stopTracking()
        }
        SettingsManager.customId = null
        _trackingId.value = generateUuid()
    }

    fun updateCustomId(id: String) {
        if (id.startsWith("@") && id.length >= 3) {
            if (connectionStatus.value == ConnectionStatus.CONNECTED) {
                stopTracking()
            }
            SettingsManager.customId = id
            _trackingId.value = id
        }
    }

    private fun startTracking() {
        val id = _trackingId.value
        tracker.startTracking(id)
        NotificationManager.show(LocalizationManager.strings.initializingBroadcast, NotificationType.INFO)
    }

    private fun stopTracking() {
        tracker.stopTracking()
        TrackingStatus.updateStatus(ConnectionStatus.IDLE)
        NotificationManager.show(LocalizationManager.strings.trackingStopped, NotificationType.INFO)
    }

    private fun generateUuid(): String {
        val chars = "0123456789abcdef"
        val id = (1..32).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        return "${id.substring(0, 8)}-${id.substring(8, 12)}-${id.substring(12, 16)}-${id.substring(16, 20)}-${id.substring(20)}"
    }
}
