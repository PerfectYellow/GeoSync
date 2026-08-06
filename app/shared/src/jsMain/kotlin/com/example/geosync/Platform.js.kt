package com.example.geosync

import web.navigator.navigator

class JsPlatform: Platform {
    private val userAgent = navigator.userAgent
    private val browserList = listOf("Chrome", "Firefox", "Safari", "Edge")

    override val name: String = userAgent.findAnyOf(browserList, ignoreCase = true)
            ?.let { (startIndex) -> userAgent.substring(startIndex).substringBefore(" ") }
            ?: "Unknown"
    override val manufacturer: String = navigator.vendor ?: "Unknown"
    override val model: String = navigator.platform ?: "Unknown"
    override val deviceName: String = "Web Browser"
    override val systemId: String = "web-${userAgent.hashCode()}"
}

actual fun getPlatform(): Platform = JsPlatform()