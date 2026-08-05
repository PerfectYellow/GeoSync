package com.example.geosync.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
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
    const val HISTORY_PATH = "/v1/history"
}

val geoHttpClient = HttpClient {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json {
            ignoreUnknownKeys = true
        })
    }
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
        })
    }
}

/**
 * Helper to fetch client history from the GeoSync server.
 */
suspend fun HttpClient.fetchClientHistory(clientId: String): List<TrackingSessionHistory> {
    val scheme = if (ApiConfig.isSecure) "https" else "http"
    val url = "$scheme://${ApiConfig.HOST}:${ApiConfig.PORT}${ApiConfig.HISTORY_PATH}/$clientId"
    return get(url).body()
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

/**
 * Helper to send location update via REST.
 */
suspend fun HttpClient.sendLocationUpdate(
    clientId: String, 
    latitude: Double, 
    longitude: Double, 
    timestamp: String?,
    isManual: Boolean = false
): String {
    val scheme = if (ApiConfig.isSecure) "https" else "http"
    val url = "$scheme://${ApiConfig.HOST}:${ApiConfig.PORT}/v1/location/$clientId"
    return post(url) {
        contentType(ContentType.Application.Json)
        setBody(LiveLocationMessage(
            type = "client.location",
            clientId = clientId,
            latitude = latitude,
            longitude = longitude,
            timestamp = timestamp,
            isManual = isManual
        ))
    }.body()
}

/**
 * Helper to notify the server that REST tracking has stopped.
 */
suspend fun HttpClient.stopLocationTracking(clientId: String): String {
    val scheme = if (ApiConfig.isSecure) "https" else "http"
    val url = "$scheme://${ApiConfig.HOST}:${ApiConfig.PORT}/v1/location/$clientId/stop"
    return post(url).body()
}
