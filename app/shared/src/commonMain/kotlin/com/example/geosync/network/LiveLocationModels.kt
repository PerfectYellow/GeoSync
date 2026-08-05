package com.example.geosync.network

import kotlinx.serialization.Serializable

@Serializable
data class LiveLocationMessage(
    val type: String,
    val clientId: String? = null,
    val deviceUuid: String? = null,
    val clientIds: List<String>? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestamp: String? = null,
    val message: String? = null,
    val isManual: Boolean? = null
)

@Serializable
data class ServerEvent(
    val type: String,
    val clientId: String? = null,
    val clientIds: List<String>? = null,
    val location: StoredLocation? = null,
    val message: String? = null,
    val subscribersCount: Int? = null
)

@Serializable
data class StoredLocation(
    val clientId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: String? = null,
    val receivedAt: String? = null,
    val isOnline: Boolean = true
)

@Serializable
data class TrackingSessionHistory(
    val id: String? = null,
    val clientId: String? = null,
    val sessionTag: String? = null,
    val totalDistanceKm: Double = 0.0,
    val startTime: String? = null,
    val endTime: String? = null,
    val points: List<HistoryPoint> = emptyList()
)

@Serializable
data class HistoryPoint(
    val id: String? = null,
    val latitude: Double,
    val longitude: Double,
    val timestamp: String? = null,
    val receivedAt: String? = null
)
