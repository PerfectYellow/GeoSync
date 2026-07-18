package com.example.geosync.network

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.*
import kotlinx.serialization.json.Json

object ApiConfig {
    enum class Environment {
        PRODUCTION,
        DEVELOPMENT
    }

    /**
     * Switch this to [Environment.DEVELOPMENT] for local testing.
     */
    private val currentEnvironment = Environment.PRODUCTION

    val HOST: String = when (currentEnvironment) {
        Environment.PRODUCTION -> "geosync.invisiblesociety.space"
        Environment.DEVELOPMENT -> "10.0.2.2" // Use "localhost" for iOS/Desktop, "10.0.2.2" for Android
    }

    val PORT: Int = when (currentEnvironment) {
        Environment.PRODUCTION -> 443
        Environment.DEVELOPMENT -> 8080
    }

    val isSecure: Boolean = PORT == 443

    const val WS_LIVE_PATH = "/v1/live"
}

val geoHttpClient = HttpClient {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json {
            ignoreUnknownKeys = true
        })
    }
}

/**
 * Helper to connect to the GeoSync live WebSocket.
 * Automatically switches between ws and wss based on the secure flag.
 */
suspend fun HttpClient.geoLiveWebSocket(block: suspend DefaultClientWebSocketSession.() -> Unit) {
    if (ApiConfig.isSecure) {
        wss(host = ApiConfig.HOST, port = ApiConfig.PORT, path = ApiConfig.WS_LIVE_PATH, block = block)
    } else {
        webSocket(host = ApiConfig.HOST, port = ApiConfig.PORT, path = ApiConfig.WS_LIVE_PATH, block = block)
    }
}
