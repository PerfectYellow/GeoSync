package com.example.geosync

class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
    override val manufacturer: String = "Web"
    override val model: String = "Wasm"
    override val deviceName: String = "Web Browser"
    override val systemId: String = "wasm-id"
}

actual fun getPlatform(): Platform = WasmPlatform()