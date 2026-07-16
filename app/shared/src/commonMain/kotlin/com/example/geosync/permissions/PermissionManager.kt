package com.example.geosync.permissions

import androidx.compose.runtime.Composable

@Composable
expect fun rememberPermissionState(
    permission: String,
    onResult: (Boolean) -> Unit
): PermissionState

interface PermissionState {
    val hasPermission: Boolean
    val shouldShowRationale: Boolean
    val isPermanentlyDenied: Boolean
    fun launchPermissionRequest()
    fun openSettings()
}

object PermissionNames {
    const val LOCATION = "location"
}
