package com.example.geosync.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
actual fun rememberPermissionState(
    permission: String,
    onResult: (Boolean) -> Unit
): PermissionState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val androidPermissions = when (permission) {
        PermissionNames.LOCATION -> arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        else -> emptyArray()
    }

    fun checkPermission(): Boolean = androidPermissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    fun checkRationale(): Boolean = androidPermissions.any {
        (context as? Activity)?.let { activity ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        } ?: false
    }

    var hasPermission by remember { mutableStateOf(checkPermission()) }
    var shouldShowRationale by remember { mutableStateOf(checkRationale()) }

    // Update state when returning from settings or foregrounding the app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = checkPermission()
                shouldShowRationale = checkRationale()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val isGranted = permissionsMap.values.any { it }
        hasPermission = isGranted
        shouldShowRationale = checkRationale()
        onResult(isGranted)
    }

    return remember(hasPermission, shouldShowRationale) {
        object : PermissionState {
            override val hasPermission: Boolean = hasPermission
            override val shouldShowRationale: Boolean = shouldShowRationale
            override val isPermanentlyDenied: Boolean = !hasPermission && !shouldShowRationale

            override fun launchPermissionRequest() {
                launcher.launch(androidPermissions)
            }

            override fun openSettings() {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }
}
