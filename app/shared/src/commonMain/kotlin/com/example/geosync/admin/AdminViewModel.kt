package com.example.geosync.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geosync.*
import com.example.geosync.localization.LocalizationManager
import com.example.geosync.network.*
import io.ktor.client.plugins.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class MapMode {
    OPEN_STREET, MAP_IR, INTERNAL, OFFLINE
}

data class MapCameraState(
    val latitude: Double,
    val longitude: Double,
    val zoom: Double
)

class AdminViewModel(private val isPreview: Boolean = false) : ViewModel() {
    private val client = geoHttpClient

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _trackedClientIds = MutableStateFlow<Set<String>>(emptySet())
    val trackedClientIds: StateFlow<Set<String>> = _trackedClientIds.asStateFlow()

    private val _locations = MutableStateFlow<Map<String, StoredLocation>>(emptyMap())
    val locations: StateFlow<Map<String, StoredLocation>> = _locations.asStateFlow()

    private val _mapMode = MutableStateFlow(MapMode.OPEN_STREET)
    val mapMode: StateFlow<MapMode> = _mapMode.asStateFlow()

    private val _clientIdInput = MutableStateFlow("")
    val clientIdInput: StateFlow<String> = _clientIdInput.asStateFlow()

    private val _isListExpanded = MutableStateFlow(false)
    val isListExpanded: StateFlow<Boolean> = _isListExpanded.asStateFlow()

    private val _isMapExpanded = MutableStateFlow(false)
    val isMapExpanded: StateFlow<Boolean> = _isMapExpanded.asStateFlow()

    private val _cameraState = MutableStateFlow(MapCameraState(35.6994, 51.3377, 11.0))
    val cameraState: StateFlow<MapCameraState> = _cameraState.asStateFlow()

    private var lastOnlineMode = MapMode.OPEN_STREET

    private var connectionJob: Job? = null
    private var session: DefaultClientWebSocketSession? = null
    
    // To track if we've already shown the connection error banner
    private var errorNotified = false

    init {
        if (!isPreview) {
            connect()
        }
    }

    fun retryConnection() {
        connect()
    }

    fun setMapMode(mode: MapMode, isOffline: Boolean = false) {
        val strings = LocalizationManager.strings
        if (isOffline && mode != MapMode.OFFLINE) {
            NotificationManager.show(strings.offlineMapChangeError, NotificationType.ERROR)
            return
        }
        
        _mapMode.value = mode
        if (mode == MapMode.OFFLINE) {
            // Force re-center to Tehran when switching to offline - Keep original zoom for offline
            _cameraState.value = MapCameraState(35.6994, 51.3377, 14.0)
        }
        
        if (!isOffline && mode != MapMode.OFFLINE) {
            lastOnlineMode = mode
        }
    }

    fun handleNetworkChange(isOffline: Boolean) {
        if (isOffline) {
            if (_mapMode.value != MapMode.OFFLINE) {
                _mapMode.value = MapMode.OFFLINE
                // Force re-center to Tehran when switching to offline due to connection loss
                _cameraState.value = MapCameraState(35.6994, 51.3377, 14.0)
            }
        } else {
            // Return to previous online mode if it was swapped to OFFLINE due to connection loss
            if (_mapMode.value == MapMode.OFFLINE) {
                _mapMode.value = lastOnlineMode
            }
        }
    }

    fun setClientIdInput(input: String) {
        _clientIdInput.value = input
    }

    fun setListExpanded(expanded: Boolean) {
        _isListExpanded.value = expanded
    }

    fun setMapExpanded(expanded: Boolean) {
        _isMapExpanded.value = expanded
    }

    fun updateCameraState(state: MapCameraState) {
        // Filter out "Null Island" initialization reports from map engines
        if (state.latitude == 0.0 && state.longitude == 0.0 && state.zoom <= 1.0) {
            return
        }
        _cameraState.value = state
    }

    private fun connect() {
        connectionJob?.cancel()
        connectionJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val strings = LocalizationManager.strings
                    client.geoLiveWebSocket {
                        session = this
                        _isConnected.value = true
                        errorNotified = false // Reset error notification state on success
                        NotificationManager.show(strings.connectedToServer, NotificationType.SUCCESS)

                        // 1. Register as admin
                        sendSerialized(LiveLocationMessage(type = "admin.register"))

                        // 2. Re-subscribe to existing clients if any
                        val currentIds = _trackedClientIds.value.toList()
                        if (currentIds.isNotEmpty()) {
                            sendSerialized(LiveLocationMessage(
                                type = "admin.subscribe",
                                clientIds = currentIds
                            ))
                        }

                        // 3. Listen for updates
                        while (isActive) {
                            val event = receiveDeserialized<ServerEvent>()
                            when (event.type) {
                                "location.update" -> {
                                    event.location?.let { loc ->
                                        val normalizedLoc = loc.copy(clientId = loc.clientId.lowercase())
                                        _locations.update { it + (normalizedLoc.clientId to normalizedLoc) }
                                    }
                                }
                                "admin.subscribed" -> {
                                    // Successfully subscribed to client(s)
                                    event.clientIds?.forEach { id ->
                                        NotificationManager.show(strings.subscribedTo(id), NotificationType.SUCCESS)
                                    }
                                }
                                "admin.unsubscribed" -> {
                                    // Successfully unsubscribed
                                }
                                "error" -> {
                                    NotificationManager.show(strings.serverError(event.message), NotificationType.ERROR)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    val strings = LocalizationManager.strings
                    _isConnected.value = false
                    // Mark all clients as offline when Admin connection is lost
                    _locations.update { current ->
                        current.mapValues { it.value.copy(isOnline = false) }
                    }
                    if (!errorNotified) {
                        NotificationManager.show(strings.connectionLost(e.message), NotificationType.ERROR)
                        errorNotified = true
                    }
                    delay(5000)
                } finally {
                    _isConnected.value = false
                    session = null
                }
            }
        }
    }

    fun addClient(rawClientId: String) {
        val clientId = rawClientId.trim().lowercase()
        println("AdminViewModel: Adding client $clientId") // Logging for debugging
        val strings = LocalizationManager.strings
        if (clientId.isBlank()) return
        
        val uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".toRegex()
        val isCustomId = clientId.startsWith("@") && clientId.length >= 3
        val isUuid = clientId.matches(uuidRegex)

        if (!isUuid && !isCustomId) {
            NotificationManager.show(strings.invalidClientIdUuid, NotificationType.ERROR)
            return
        }
        
        if (_trackedClientIds.value.contains(clientId)) {
            NotificationManager.show(strings.clientAlreadyTracked(clientId), NotificationType.INFO)
            return
        }

        _trackedClientIds.update { it + clientId }
        
        viewModelScope.launch {
            try {
                if (session != null) {
                    session?.sendSerialized(LiveLocationMessage(
                        type = "admin.subscribe",
                        clientIds = listOf(clientId)
                    ))
                } else {
                    NotificationManager.show(strings.waitingForConnection, NotificationType.INFO)
                }
            } catch (e: Exception) {
                NotificationManager.show(strings.failedToSubscribe(e.message), NotificationType.ERROR)
            }
        }
    }

    fun removeClient(rawClientId: String) {
        val clientId = rawClientId.lowercase()
        val strings = LocalizationManager.strings
        _trackedClientIds.update { it - clientId }
        _locations.update { it - clientId }

        viewModelScope.launch {
            try {
                session?.sendSerialized(LiveLocationMessage(
                    type = "admin.unsubscribe",
                    clientIds = listOf(clientId)
                ))
                NotificationManager.show(strings.removedClient(clientId), NotificationType.INFO)
            } catch (e: Exception) {
                NotificationManager.show(strings.failedToUnsubscribe(e.message), NotificationType.ERROR)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        connectionJob?.cancel()
    }
}
