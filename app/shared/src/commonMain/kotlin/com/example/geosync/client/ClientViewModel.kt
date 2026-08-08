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
    val restProgress = TrackingStatus.restProgress

    private val _trackingId = MutableStateFlow(SettingsManager.customId ?: SettingsManager.deviceUuid)
    val trackingId: StateFlow<String> = _trackingId.asStateFlow()

    init { }

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
        _trackingId.value = SettingsManager.generateUuid()
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

    fun manualUpdate() {
        if (connectionStatus.value == ConnectionStatus.CONNECTED) {
            tracker.manualUpdate()
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
}
