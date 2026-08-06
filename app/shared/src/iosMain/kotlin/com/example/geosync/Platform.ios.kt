package com.example.geosync

import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val manufacturer: String = "Apple"
    override val model: String = UIDevice.currentDevice.model
    override val deviceName: String = UIDevice.currentDevice.name
    override val systemId: String = UIDevice.currentDevice.identifierForVendor?.UUIDString ?: ""
}

actual fun getPlatform(): Platform = IOSPlatform()