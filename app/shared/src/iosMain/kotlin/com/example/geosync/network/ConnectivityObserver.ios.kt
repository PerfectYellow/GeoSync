package com.example.geosync.network

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class IosConnectivityObserver : ConnectivityObserver {
    override fun observe(): Flow<ConnectivityStatus> = flowOf(ConnectivityStatus.Online)
}

@Composable
actual fun rememberConnectivityObserver(): ConnectivityObserver = remember { IosConnectivityObserver() }
