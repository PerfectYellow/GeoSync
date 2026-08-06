package com.example.geosync

interface Platform {
    val name: String
    val manufacturer: String
    val model: String
    val deviceName: String
    val systemId: String
}

expect fun getPlatform(): Platform