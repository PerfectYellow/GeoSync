package com.example.geosync

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
    override val manufacturer: String = System.getProperty("os.name") ?: "Unknown"
    override val model: String = System.getProperty("os.version") ?: "Unknown"
    override val deviceName: String = System.getProperty("user.name") ?: "Unknown"
    override val systemId: String = "jvm-${System.getProperty("user.home").hashCode()}"
}

actual fun getPlatform(): Platform = JVMPlatform()