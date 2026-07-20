package com.example.geosync.admin

import androidx.compose.ui.graphics.Color

object AdminUtils {
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
}
