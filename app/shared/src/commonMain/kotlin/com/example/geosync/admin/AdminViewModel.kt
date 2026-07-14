package com.example.geosync.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geosync.*
import com.example.geosync.network.*
import io.ktor.client.plugins.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class AdminViewModel : ViewModel() {
    private val client = geoHttpClient

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _trackedClientIds = MutableStateFlow<Set<String>>(emptySet())
    val trackedClientIds: StateFlow<Set<String>> = _trackedClientIds.asStateFlow()

    private val _locations = MutableStateFlow<Map<String, StoredLocation>>(emptyMap())
    val locations: StateFlow<Map<String, StoredLocation>> = _locations.asStateFlow()

    private var connectionJob: Job? = null
    private var session: DefaultClientWebSocketSession? = null
    
    // To track if we've already shown the connection error banner
    private var errorNotified = false

    init {
        connect()
    }

    fun retryConnection() {
        connect()
    }

    private fun connect() {
        connectionJob?.cancel()
        connectionJob = viewModelScope.launch {
            while (isActive) {
                try {
                    client.webSocket(
                        host = ApiConfig.HOST,
                        port = ApiConfig.PORT,
                        path = ApiConfig.WS_LIVE_PATH
                    ) {
                        session = this
                        _isConnected.value = true
                        errorNotified = false // Reset error notification state on success
                        NotificationManager.show("Connected to Server", NotificationType.SUCCESS)

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
                                        NotificationManager.show("Subscribed to $id", NotificationType.SUCCESS)
                                    }
                                }
                                "admin.unsubscribed" -> {
                                    // Successfully unsubscribed
                                }
                                "error" -> {
                                    NotificationManager.show("Server error: ${event.message}", NotificationType.ERROR)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    _isConnected.value = false
                    if (!errorNotified) {
                        NotificationManager.show("Connection lost: ${e.message}", NotificationType.ERROR)
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
        if (clientId.isBlank()) return
        
        val uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".toRegex()
        if (!clientId.matches(uuidRegex)) {
            NotificationManager.show("Invalid Client ID format. Must be a valid UUID.", NotificationType.ERROR)
            return
        }
        
        if (_trackedClientIds.value.contains(clientId)) {
            NotificationManager.show("Client $clientId is already being tracked", NotificationType.INFO)
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
                    NotificationManager.show("Waiting for connection...", NotificationType.INFO)
                }
            } catch (e: Exception) {
                NotificationManager.show("Failed to subscribe: ${e.message}", NotificationType.ERROR)
            }
        }
    }

    fun removeClient(rawClientId: String) {
        val clientId = rawClientId.lowercase()
        _trackedClientIds.update { it - clientId }
        _locations.update { it - clientId }

        viewModelScope.launch {
            try {
                session?.sendSerialized(LiveLocationMessage(
                    type = "admin.unsubscribe",
                    clientIds = listOf(clientId)
                ))
                NotificationManager.show("Removed $clientId", NotificationType.INFO)
            } catch (e: Exception) {
                NotificationManager.show("Failed to unsubscribe: ${e.message}", NotificationType.ERROR)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        connectionJob?.cancel()
    }
}
