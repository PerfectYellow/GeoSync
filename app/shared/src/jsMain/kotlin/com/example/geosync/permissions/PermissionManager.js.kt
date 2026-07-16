package com.example.geosync.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberPermissionState(
    permission: String,
    onResult: (Boolean) -> Unit
): PermissionState {
    return remember {
        object : PermissionState {
            override val hasPermission: Boolean = true
            override val shouldShowRationale: Boolean = false
            override val isPermanentlyDenied: Boolean = false
            override fun launchPermissionRequest() {
                onResult(true)
            }
            override fun openSettings() {}
        }
    }
}
