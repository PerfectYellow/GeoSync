package com.example.geosync.permissions

import androidx.compose.runtime.Composable

@Composable
expect fun rememberPermissionState(
    permission: String,
    onResult: (Boolean) -> Unit
): PermissionState

interface PermissionState {
    val hasPermission: Boolean
    fun launchPermissionRequest()
}

object PermissionNames {
    const val LOCATION = "location"
}
