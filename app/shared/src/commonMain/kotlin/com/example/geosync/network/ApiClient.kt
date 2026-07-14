package com.example.geosync.network

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.*
import kotlinx.serialization.json.Json

object ApiConfig {
    /**
     * The host address for the GeoSync server.
     * 
     * - Use "localhost" for Desktop/iOS Simulator.
     * - Use "10.0.2.2" for Android Emulator.
     * - Use your machine's IP address (e.g., "192.168.1.x") if testing on a real device.
     */
    const val HOST = "10.0.2.2"
    const val PORT = 8080
    const val WS_LIVE_PATH = "/v1/live"
}

val geoHttpClient = HttpClient {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json {
            ignoreUnknownKeys = true
        })
    }
}
