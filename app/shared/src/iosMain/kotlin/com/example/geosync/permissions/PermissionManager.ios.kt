package com.example.geosync.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberPermissionState(
    permission: String,
    onResult: (Boolean) -> Unit
): PermissionState {
    // Dummy implementation for iOS
    return remember {
        object : PermissionState {
            override val hasPermission: Boolean = true
            override fun launchPermissionRequest() {
                onResult(true)
            }
        }
    }
}
