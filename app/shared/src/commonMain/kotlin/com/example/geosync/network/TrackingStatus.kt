package com.example.geosync.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConnectionStatus {
    IDLE,
    CONNECTING,
    CONNECTED,
    FAILED
}

object TrackingStatus {
    private val _status = MutableStateFlow(ConnectionStatus.IDLE)
    val status = _status.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _subscribersCount = MutableStateFlow(0)
    val subscribersCount = _subscribersCount.asStateFlow()

    fun updateStatus(newStatus: ConnectionStatus, error: String? = null) {
        _status.value = newStatus
        _errorMessage.value = error
    }

    fun updateSubscribers(count: Int) {
        _subscribersCount.value = count
    }
}
