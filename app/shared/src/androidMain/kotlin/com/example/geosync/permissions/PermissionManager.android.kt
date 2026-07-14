package com.example.geosync.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun rememberPermissionState(
    permission: String,
    onResult: (Boolean) -> Unit
): PermissionState {
    val context = LocalContext.current
    val androidPermission = when (permission) {
        PermissionNames.LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION
        else -> ""
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                androidPermission
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        onResult(isGranted)
    }

    return remember(hasPermission) {
        object : PermissionState {
            override val hasPermission: Boolean = hasPermission
            override fun launchPermissionRequest() {
                launcher.launch(androidPermission)
            }
        }
    }
}
