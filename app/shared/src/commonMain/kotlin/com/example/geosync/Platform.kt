package com.example.geosync

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform