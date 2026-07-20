package com.example.geosync.localization

import androidx.compose.runtime.*
import androidx.compose.ui.unit.LayoutDirection
import com.russhwolf.settings.Settings

interface AppStrings {
    val adminPortal: String
    val clientPortal: String
    val locationSynchronization: String
    val startTracking: String
    val stopTracking: String
    val connectedToServer: String
    val youAreOffline: String
    val offlineTapToRetry: String
    val clientIdToTrack: String
    val enterClientId: String
    val addClient: String
    val invalidUuidFormat: String
    val trackedClients: (Int) -> String
    val noClientsAdded: String
    val statusOffline: String
    val statusDisconnected: String
    val statusWaiting: String
    val statusLive: String
    val adminConnectionLost: String
    val lastSeen: String
    val waitingForLocationSync: String
    val remove: String
    val mapPreview: String
    val collapseMap: String
    val expandMap: String
    val permissionBlocked: String
    val locationAccessRequired: String
    val locationPermissionDeniedPermanently: String
    val locationPermissionRationale: String
    val openSettings: String
    val grantPermission: String
    val cancel: String
    val dataVisibilityInfo: String
    val readyToSync: String
    val startBroadcastingDesc: String
    val nowTracking: String
    val sessionUuid: String
    val tapToCopyId: String
    val initializingBroadcast: String
    val trackingStopped: String
    val trackingActive: (String) -> String
    val trackingActiveWithAccuracy: (String, Int) -> String
    val gpsDisabled: String
    val gpsOffEnableIt: String
    val geoSyncInitializing: String
    val locationPermissionDenied: String
    val connectedToRelay: String
    val waitingForGpsFix: String
    val connectionFailed: (String?) -> String
    val serverError: (String?) -> String
    val connectionLost: (String?) -> String
    val invalidClientIdUuid: String
    val clientAlreadyTracked: (String) -> String
    val waitingForConnection: String
    val failedToSubscribe: (String?) -> String
    val removedClient: (String) -> String
    val failedToUnsubscribe: (String?) -> String
    val subscribedTo: (String) -> String
    val admin: String
    val client: String
    val dismiss: String
    val locationTrackingChannel: String
    val geoSyncTrackingTitle: String
    val mapOpenStreet: String
    val mapMapIr: String
    val mapInternal: String
    val mapOffline: String
    val offlineMapChangeError: String
    val adminSubscribed: (Int) -> String
    val backgroundLocationRationale: String
    val backgroundLocationWarning: String
    val batteryOptimizationWarning: String
    val fix: String
}

object EnStrings : AppStrings {
    override val adminPortal = "Admin Portal"
    override val clientPortal = "Client Portal"
    override val locationSynchronization = "Location Synchronization"
    override val startTracking = "Start Tracking"
    override val stopTracking = "Stop Tracking"
    override val connectedToServer = "Connected to Server"
    override val youAreOffline = "You are offline"
    override val offlineTapToRetry = "Offline - Tap to Retry"
    override val clientIdToTrack = "Client ID to Track"
    override val enterClientId = "Enter client ID..."
    override val addClient = "Add Client"
    override val invalidUuidFormat = "Invalid UUID format (e.g. 123e4567-e89b...)"
    override val trackedClients: (Int) -> String = { count -> "Tracked Clients ($count)" }
    override val noClientsAdded = "No clients added yet"
    override val statusOffline = "OFFLINE"
    override val statusDisconnected = "DISCONNECTED"
    override val statusWaiting = "WAITING"
    override val statusLive = "LIVE"
    override val adminConnectionLost = "Admin connection lost"
    override val lastSeen = "Last seen"
    override val waitingForLocationSync = "Waiting for location sync..."
    override val remove = "Remove"
    override val mapPreview = "Map Preview"
    override val collapseMap = "Collapse Map"
    override val expandMap = "Expand Map"
    override val permissionBlocked = "Permission Blocked"
    override val locationAccessRequired = "Location Access Required"
    override val locationPermissionDeniedPermanently = "Location permission is permanently denied. Please enable it in app settings to use tracking."
    override val locationPermissionRationale = "This app needs your location to synchronize it with the admin dashboard in real-time."
    override val openSettings = "Open Settings"
    override val grantPermission = "Grant Permission"
    override val cancel = "Cancel"
    override val dataVisibilityInfo = "Your data is only visible to authorized administrators."
    override val readyToSync = "Ready to Sync?"
    override val startBroadcastingDesc = "Press the button below to start broadcasting your position to the network."
    override val nowTracking = "now your location tracking"
    override val sessionUuid = "SESSION UUID"
    override val tapToCopyId = "Tap to copy ID"
    override val initializingBroadcast = "Initializing broadcast..."
    override val trackingStopped = "Tracking stopped"
    override val trackingActive: (String) -> String = { id -> "Tracking active: $id" }
    override val trackingActiveWithAccuracy: (String, Int) -> String = { provider, acc -> "Tracking active: $provider (${acc}m)" }
    override val gpsDisabled = "GPS Disabled"
    override val gpsOffEnableIt = "GPS is turned off. Please enable it."
    override val geoSyncInitializing = "GeoSync is initializing..."
    override val locationPermissionDenied = "Location permission denied"
    override val connectedToRelay = "Connected to relay"
    override val waitingForGpsFix = "Waiting for GPS fix..."
    override val connectionFailed: (String?) -> String = { msg -> "Connection failed: $msg" }
    override val serverError: (String?) -> String = { msg -> "Server error: $msg" }
    override val connectionLost: (String?) -> String = { msg -> "Connection lost: $msg" }
    override val invalidClientIdUuid = "Invalid Client ID format. Must be a valid UUID."
    override val clientAlreadyTracked: (String) -> String = { id -> "Client $id is already being tracked" }
    override val waitingForConnection = "Waiting for connection..."
    override val failedToSubscribe: (String?) -> String = { msg -> "Failed to subscribe: $msg" }
    override val removedClient: (String) -> String = { id -> "Removed $id" }
    override val failedToUnsubscribe: (String?) -> String = { msg -> "Failed to unsubscribe: $msg" }
    override val subscribedTo: (String) -> String = { id -> "Subscribed to $id" }
    override val admin = "Admin"
    override val client = "Client"
    override val dismiss = "Dismiss"
    override val locationTrackingChannel = "Location Tracking"
    override val geoSyncTrackingTitle = "GeoSync Tracking"
    override val mapOpenStreet = "Open Street"
    override val mapMapIr = "Map.ir"
    override val mapInternal = "Internal"
    override val mapOffline = "Offline"
    override val offlineMapChangeError = "You are offline and can't change map"
    override val adminSubscribed: (Int) -> String = { count -> if (count == 1) "1 admin is watching" else "$count admins are watching" }
    override val backgroundLocationRationale = "For maximum reliability, please set location permission to 'Allow all the time'. This ensures tracking continues when you get a call or the screen is off."
    override val backgroundLocationWarning = "Background tracking might be limited. 'Allow all the time' is recommended."
    override val batteryOptimizationWarning = "Battery optimization is active. Tracking might be stopped by the system. Please disable it for this app."
    override val fix = "Fix"
}

object FaStrings : AppStrings {
    override val adminPortal = "پورتال مدیریت"
    override val clientPortal = "پورتال کاربر"
    override val locationSynchronization = "همگام‌سازی مکان"
    override val startTracking = "شروع ردیابی"
    override val stopTracking = "توقف ردیابی"
    override val connectedToServer = "متصل به سرور"
    override val youAreOffline = "شما آفلاین هستید"
    override val offlineTapToRetry = "آفلاین - برای تلاش مجدد ضربه بزنید"
    override val clientIdToTrack = "شناسه کاربر برای ردیابی"
    override val enterClientId = "شناسه کاربر را وارد کنید..."
    override val addClient = "افزودن کاربر"
    override val invalidUuidFormat = "فرمت UUID نامعتبر است (مثلاً 123e4567-e89b...)"
    override val trackedClients: (Int) -> String = { count -> "کاربران ردیابی شده ($count)" }
    override val noClientsAdded = "هنوز کاربری اضافه نشده است"
    override val statusOffline = "آفلاین"
    override val statusDisconnected = "قطع شده"
    override val statusWaiting = "در انتظار"
    override val statusLive = "زنده"
    override val adminConnectionLost = "اتصال مدیریت قطع شد"
    override val lastSeen = "آخرین مشاهده"
    override val waitingForLocationSync = "در انتظار همگام‌سازی مکان..."
    override val remove = "حذف"
    override val mapPreview = "پیش‌نمایش نقشه"
    override val collapseMap = "بستن نقشه"
    override val expandMap = "بزرگ‌نمایی نقشه"
    override val permissionBlocked = "دسترسی مسدود شد"
    override val locationAccessRequired = "دسترسی به مکان لازم است"
    override val locationPermissionDeniedPermanently = "دسترسی به مکان به طور دائم رد شده است. لطفاً آن را در تنظیمات برنامه برای استفاده از ردیابی فعال کنید."
    override val locationPermissionRationale = "این برنامه برای همگام‌سازی موقعیت شما با داشبورد مدیریت به صورت لحظه‌ای، به دسترسی مکان نیاز دارد."
    override val openSettings = "باز کردن تنظیمات"
    override val grantPermission = "دادن دسترسی"
    override val cancel = "لغو"
    override val dataVisibilityInfo = "داده‌های شما فقط برای مدیران مجاز قابل مشاهده است."
    override val readyToSync = "آماده همگام‌سازی هستید؟"
    override val startBroadcastingDesc = "برای شروع ارسال موقعیت خود به شبکه، دکمه زیر را فشار دهید."
    override val nowTracking = "در حال ردیابی مکان شما"
    override val sessionUuid = "شناسه نشست (UUID)"
    override val tapToCopyId = "برای کپی کردن شناسه ضربه بزنید"
    override val initializingBroadcast = "در حال راه‌اندازی ارسال..."
    override val trackingStopped = "ردیابی متوقف شد"
    override val trackingActive: (String) -> String = { id -> "ردیابی فعال: $id" }
    override val trackingActiveWithAccuracy: (String, Int) -> String = { provider, acc -> "ردیابی فعال: $provider ($acc متر)" }
    override val gpsDisabled = "جی‌پی‌اس غیرفعال است"
    override val gpsOffEnableIt = "جی‌پی‌اس خاموش است. لطفاً آن را فعال کنید."
    override val geoSyncInitializing = "GeoSync در حال راه‌اندازی است..."
    override val locationPermissionDenied = "دسترسی به مکان رد شد"
    override val connectedToRelay = "متصل به رله"
    override val waitingForGpsFix = "در انتظار موقعیت جی‌پی‌اس..."
    override val connectionFailed: (String?) -> String = { msg -> "اتصال ناموفق بود: $msg" }
    override val serverError: (String?) -> String = { msg -> "خطای سرور: $msg" }
    override val connectionLost: (String?) -> String = { msg -> "اتصال قطع شد: $msg" }
    override val invalidClientIdUuid = "فرمت شناسه کاربر نامعتبر است. باید یک UUID معتبر باشد."
    override val clientAlreadyTracked: (String) -> String = { id -> "کاربر $id در حال ردیابی است" }
    override val waitingForConnection = "در انتظار اتصال..."
    override val failedToSubscribe: (String?) -> String = { msg -> "اشتراک ناموفق بود: $msg" }
    override val removedClient: (String) -> String = { id -> "$id حذف شد" }
    override val failedToUnsubscribe: (String?) -> String = { msg -> "لغو اشتراک ناموفق بود: $msg" }
    override val subscribedTo: (String) -> String = { id -> "اشتراک در $id برقرار شد" }
    override val admin = "مدیریت"
    override val client = "کاربر"
    override val dismiss = "بستن"
    override val locationTrackingChannel = "ردیابی مکان"
    override val geoSyncTrackingTitle = "ردیابی GeoSync"
    override val mapOpenStreet = "Open Street"
    override val mapMapIr = "Map.ir"
    override val mapInternal = "داخلی"
    override val mapOffline = "آفلاین"
    override val offlineMapChangeError = "شما آفلاین هستید و نمی‌توانید نقشه را تغییر دهید"
    override val adminSubscribed: (Int) -> String = { count -> if (count == 1) "۱ مدیر در حال مشاهده است" else "$count مدیر در حال مشاهده هستند" }
    override val backgroundLocationRationale = "برای اطمینان حداکثری، لطفاً دسترسی به مکان را روی «همیشه اجازه داده شود» تنظیم کنید. این کار باعث می‌شود ردیابی هنگام تماس یا خاموش بودن صفحه ادامه یابد."
    override val backgroundLocationWarning = "ردیابی در پس‌زمینه ممکن است محدود باشد. تنظیم «همیشه اجازه داده شود» توصیه می‌شود."
    override val batteryOptimizationWarning = "بهینه‌سازی باتری فعال است. ممکن است سیستم ردیابی را متوقف کند. لطفاً آن را برای این برنامه غیرفعال کنید."
    override val fix = "بررسی"
}

enum class Language(val code: String, val label: String, val flag: String) {
    ENGLISH("en", "English", "🇺🇸"),
    PERSIAN("fa", "فارسی", "🇮🇷")
}

object LocalizationManager {
    private val settings: Settings by lazy {
        try {
            Settings()
        } catch (e: Exception) {
            object : Settings {
                override val keys: Set<String> get() = emptySet()
                override val size: Int get() = 0
                override fun clear() {}
                override fun remove(key: String) {}
                override fun hasKey(key: String): Boolean = false
                override fun putInt(key: String, value: Int) {}
                override fun getInt(key: String, defaultValue: Int): Int = defaultValue
                override fun putLong(key: String, value: Long) {}
                override fun getLong(key: String, defaultValue: Long): Long = defaultValue
                override fun putString(key: String, value: String) {}
                override fun getString(key: String, defaultValue: String): String = defaultValue
                override fun putFloat(key: String, value: Float) {}
                override fun getFloat(key: String, defaultValue: Float): Float = defaultValue
                override fun putDouble(key: String, value: Double) {}
                override fun getDouble(key: String, defaultValue: Double): Double = defaultValue
                override fun putBoolean(key: String, value: Boolean) {}
                override fun getBoolean(key: String, defaultValue: Boolean): Boolean = defaultValue
                override fun getIntOrNull(key: String): Int? = null
                override fun getLongOrNull(key: String): Long? = null
                override fun getStringOrNull(key: String): String? = null
                override fun getFloatOrNull(key: String): Float? = null
                override fun getDoubleOrNull(key: String): Double? = null
                override fun getBooleanOrNull(key: String): Boolean? = null
            }
        }
    }
    private const val KEY_LANGUAGE = "selected_language"

    private var _currentLanguage by mutableStateOf(loadLanguage())
    var currentLanguage: Language
        get() = _currentLanguage
        set(value) {
            _currentLanguage = value
            try {
                settings.putString(KEY_LANGUAGE, value.code)
            } catch (e: Exception) {
                // Ignore errors in environments where settings might fail (like previews)
            }
        }
    
    val strings: AppStrings
        get() = when (currentLanguage) {
            Language.ENGLISH -> EnStrings
            Language.PERSIAN -> FaStrings
        }

    val layoutDirection: LayoutDirection
        get() = when (currentLanguage) {
            Language.ENGLISH -> LayoutDirection.Ltr
            Language.PERSIAN -> LayoutDirection.Rtl
        }

    fun toggleLanguage() {
        currentLanguage = if (currentLanguage == Language.ENGLISH) Language.PERSIAN else Language.ENGLISH
    }

    private fun loadLanguage(): Language {
        val savedCode = try {
            settings.getString(KEY_LANGUAGE, Language.ENGLISH.code)
        } catch (e: Exception) {
            Language.ENGLISH.code
        }
        return Language.entries.find { it.code == savedCode } ?: Language.ENGLISH
    }
}

val LocalStrings = staticCompositionLocalOf<AppStrings> { EnStrings }
