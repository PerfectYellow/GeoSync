package com.example.geosync.admin

import androidx.compose.ui.graphics.Color
import kotlinx.datetime.*

object AdminUtils {
    /**
     * Robust time extractor that handles UTC-to-Local conversion and various string formats.
     * Returns HH:mm
     */
    fun formatToLocalTime(ts: String?): String {
        if (ts == null || ts.isBlank()) return "--:--"
        
        // Handle Unix timestamp (number as string) if server sends it as a double
        if (ts.all { it.isDigit() || it == '.' }) {
            return try {
                val seconds = ts.toDouble().toLong()
                val instant = Instant.fromEpochSeconds(seconds)
                val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
            } catch (e: Exception) {
                "--:--"
            }
        }

        return try {
            val normalized = ts.replace(" ", "T")
            if (normalized.contains("T")) {
                val iso = if (!normalized.contains("+") && !normalized.endsWith("Z")) "${normalized}Z" else normalized
                val inst = Instant.parse(iso)
                val local = inst.toLocalDateTime(TimeZone.currentSystemDefault())
                "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
            } else {
                if (normalized.contains(":")) normalized.take(5) else "--:--"
            }
        } catch (e: Exception) {
            "(\\d{2}:\\d{2})".toRegex().find(ts ?: "")?.value ?: "--:--"
        }
    }

    /**
     * Extracts and formats the date part from a timestamp.
     * Returns YYYY-MM-DD
     */
    fun formatToLocalDate(ts: String?): String {
        if (ts == null || ts.isBlank()) return "---"
        return try {
            val normalized = ts.replace(" ", "T")
            if (normalized.contains("T")) {
                val iso = if (!normalized.contains("+") && !normalized.endsWith("Z")) "${normalized}Z" else normalized
                val inst = Instant.parse(iso)
                val local = inst.toLocalDateTime(TimeZone.currentSystemDefault())
                "${local.year}-${local.monthNumber.toString().padStart(2, '0')}-${local.dayOfMonth.toString().padStart(2, '0')}"
            } else {
                normalized.substringBefore("T").ifEmpty { "---" }
            }
        } catch (e: Exception) {
            "---"
        }
    }

    /**
     * A list of distinct, vibrant colors for client markers and list items.
     */
    private val clientColors = listOf(
        0xFF1E88E5, // Blue
        0xFFE53935, // Red
        0xFF43A047, // Green
        0xFFFB8C00, // Orange
        0xFF8E24AA, // Purple
        0xFF00ACC1, // Cyan
        0xFFD81B60, // Pink
        0xFF795548, // Brown
        0xFF3949AB, // Indigo
        0xFF00897B  // Teal
    )

    /**
     * Returns a stable color for a given clientId based on its hash.
     */
    fun getClientColor(clientId: String): Color {
        val hash = clientId.hashCode()
        val index = (hash % clientColors.size).let { if (it < 0) it + clientColors.size else it }
        return Color(clientColors[index])
    }

    /**
     * Calculates the total distance of a path in Kilometers using the Haversine formula.
     */
    fun calculatePathDistance(points: List<com.example.geosync.network.HistoryPoint>): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 0 until points.size - 1) {
            total += haversineDistance(
                points[i].latitude, points[i].longitude,
                points[i + 1].latitude, points[i + 1].longitude
            )
        }
        return total
    }

    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = (lat2 - lat1) * kotlin.math.PI / 180.0
        val dLon = (lon2 - lon1) * kotlin.math.PI / 180.0
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(lat1 * kotlin.math.PI / 180.0) * 
                kotlin.math.cos(lat2 * kotlin.math.PI / 180.0) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return r * c
    }
}
