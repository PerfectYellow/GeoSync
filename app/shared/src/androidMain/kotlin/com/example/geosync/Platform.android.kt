package com.example.geosync

import android.os.Build
import android.provider.Settings
import com.example.geosync.network.androidContext

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val manufacturer: String = Build.MANUFACTURER
    override val model: String = Build.MODEL
    override val deviceName: String = try {
        Settings.Global.getString(androidContext.contentResolver, Settings.Global.DEVICE_NAME)
            ?: Settings.Secure.getString(androidContext.contentResolver, "bluetooth_name")
            ?: ""
    } catch (e: Exception) {
        ""
    }
    override val systemId: String = try {
        Settings.Secure.getString(androidContext.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
    } catch (e: Exception) {
        ""
    }
}

actual fun getPlatform(): Platform = AndroidPlatform()
