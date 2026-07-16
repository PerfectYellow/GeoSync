package com.example.geosync.network

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

enum class ConnectivityStatus {
    Online, Offline
}

interface ConnectivityObserver {
    fun observe(): Flow<ConnectivityStatus>
}

@Composable
expect fun rememberConnectivityObserver(): ConnectivityObserver
