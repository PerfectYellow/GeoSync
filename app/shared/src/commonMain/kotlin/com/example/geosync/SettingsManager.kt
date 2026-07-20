package com.example.geosync

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

object SettingsManager {
    private val settings: Settings by lazy {
        try {
            Settings()
        } catch (e: Exception) {
            // Fallback for environments where settings might fail (e.g. some tests or previews)
            object : Settings {
                private val map = mutableMapOf<String, Any>()
                override val keys: Set<String> get() = map.keys
                override val size: Int get() = map.size
                override fun clear() = map.clear()
                override fun remove(key: String) { map.remove(key) }
                override fun hasKey(key: String): Boolean = map.containsKey(key)
                override fun putInt(key: String, value: Int) { map[key] = value }
                override fun getInt(key: String, defaultValue: Int): Int = map[key] as? Int ?: defaultValue
                override fun putLong(key: String, value: Long) { map[key] = value }
                override fun getLong(key: String, defaultValue: Long): Long = map[key] as? Long ?: defaultValue
                override fun putString(key: String, value: String) { map[key] = value }
                override fun getString(key: String, defaultValue: String): String = map[key] as? String ?: defaultValue
                override fun putFloat(key: String, value: Float) { map[key] = value }
                override fun getFloat(key: String, defaultValue: Float): Float = map[key] as? Float ?: defaultValue
                override fun putDouble(key: String, value: Double) { map[key] = value }
                override fun getDouble(key: String, defaultValue: Double): Double = map[key] as? Double ?: defaultValue
                override fun putBoolean(key: String, value: Boolean) { map[key] = value }
                override fun getBoolean(key: String, defaultValue: Boolean): Boolean = map[key] as? Boolean ?: defaultValue
                override fun getIntOrNull(key: String): Int? = map[key] as? Int
                override fun getLongOrNull(key: String): Long? = map[key] as? Long
                override fun getStringOrNull(key: String): String? = map[key] as? String
                override fun getFloatOrNull(key: String): Float? = map[key] as? Float
                override fun getDoubleOrNull(key: String): Double? = map[key] as? Double
                override fun getBooleanOrNull(key: String): Boolean? = map[key] as? Boolean
            }
        }
    }

    private const val KEY_CUSTOM_ID = "custom_tracking_id"

    var customId: String?
        get() = settings.getStringOrNull(KEY_CUSTOM_ID)
        set(value) {
            if (value == null) {
                settings.remove(KEY_CUSTOM_ID)
            } else {
                settings.putString(KEY_CUSTOM_ID, value)
            }
        }
}
