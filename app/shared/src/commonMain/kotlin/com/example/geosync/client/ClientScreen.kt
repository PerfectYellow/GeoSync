package com.example.geosync.client

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import com.example.geosync.localization.LocalStrings
import com.example.geosync.permissions.PermissionNames
import com.example.geosync.permissions.rememberPermissionState
import com.example.geosync.network.ConnectionStatus
import com.example.geosync.network.ConnectivityStatus
import com.example.geosync.network.rememberConnectivityObserver
import com.example.geosync.network.isIgnoringBatteryOptimizations
import com.example.geosync.network.openBatteryOptimizationSettings
import com.example.geosync.LanguageSelector
import com.example.geosync.NotificationBanner
import com.example.geosync.NotificationManager
import com.example.geosync.SettingsManager
import com.example.geosync.getPlatform

@Composable
fun ClientScreen(
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val isPreview = LocalInspectionMode.current
    val viewModel: ClientViewModel = viewModel { ClientViewModel(isPreview) }
    
    val trackingId by viewModel.trackingId.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val connectionError by viewModel.connectionError.collectAsState()
    val subscribersCount by viewModel.subscribersCount.collectAsState()
    val restProgress by viewModel.restProgress.collectAsState()

    val connectivityObserver = rememberConnectivityObserver()
    val networkStatus by connectivityObserver.observe().collectAsState(ConnectivityStatus.Online)

    val backgroundLocationPermissionState = rememberPermissionState(
        permission = PermissionNames.BACKGROUND_LOCATION,
        onResult = {}
    )

    var isBatteryOptimized by remember { mutableStateOf(!isIgnoringBatteryOptimizations()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isBatteryOptimized = !isIgnoringBatteryOptimizations()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showPermissionRationale by remember { mutableStateOf(false) }
    var showBackgroundRationale by remember { mutableStateOf(false) }
    var showDeviceDetails by remember { mutableStateOf(false) }
    var showConnectionTypePicker by remember { mutableStateOf(false) }
    var showStopConfirmation by remember { mutableStateOf(false) }

    val locationPermissionState = rememberPermissionState(
        permission = PermissionNames.LOCATION,
        onResult = { isGranted ->
            if (isGranted) {
                if (networkStatus == ConnectivityStatus.Offline) {
                    NotificationManager.showOffline()
                } else {
                    viewModel.toggleTracking()
                }
            }
        }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        ClientScreenContent(
            trackingId = trackingId,
            connectionStatus = connectionStatus,
            connectionError = connectionError,
            subscribersCount = subscribersCount,
            restProgress = restProgress,
            hasBackgroundPermission = backgroundLocationPermissionState.hasPermission,
            isBatteryOptimized = isBatteryOptimized,
            onToggleTracking = {
                if (networkStatus == ConnectivityStatus.Offline) {
                    NotificationManager.showOffline()
                } else if (connectionStatus == ConnectionStatus.CONNECTED || connectionStatus == ConnectionStatus.CONNECTING) {
                    showStopConfirmation = true
                } else if (locationPermissionState.hasPermission) {
                    viewModel.toggleTracking()
                } else {
                    showPermissionRationale = true
                }
            },
            onRefreshId = { viewModel.refreshTrackingId() },
            onUpdateCustomId = { viewModel.updateCustomId(it) },
            onRequestBackgroundPermission = {
                showBackgroundRationale = true
            },
            onFixBatteryOptimization = {
                openBatteryOptimizationSettings()
            },
            onShowDeviceDetails = { showDeviceDetails = true },
            onShowConnectionType = { showConnectionTypePicker = true },
            onManualUpdate = { viewModel.manualUpdate() },
            paddingValues = paddingValues
        )

        if (showStopConfirmation) {
            StopConfirmationDialog(
                onDismiss = { showStopConfirmation = false },
                onConfirm = {
                    showStopConfirmation = false
                    viewModel.toggleTracking()
                }
            )
        }

        if (showDeviceDetails) {
            DeviceDetailsDialog(onDismiss = { showDeviceDetails = false })
        }

        if (showConnectionTypePicker) {
            ConnectionTypeDialog(
                onDismiss = { showConnectionTypePicker = false },
                onSet = { selectedType ->
                    SettingsManager.connectionType = selectedType
                    showConnectionTypePicker = false
                }
            )
        }

        if (showPermissionRationale) {
            PermissionDialog(
                isPermanentlyDenied = locationPermissionState.isPermanentlyDenied,
                onDismiss = { showPermissionRationale = false },
                onGrant = {
                    showPermissionRationale = false
                    locationPermissionState.launchPermissionRequest()
                },
                onOpenSettings = {
                    showPermissionRationale = false
                    locationPermissionState.openSettings()
                }
            )
        }

        if (showBackgroundRationale) {
            val strings = LocalStrings.current
            AlertDialog(
                onDismissRequest = { showBackgroundRationale = false },
                title = { Text(strings.locationAccessRequired, fontWeight = FontWeight.Bold) },
                text = { Text(strings.backgroundLocationRationale) },
                confirmButton = {
                    Button(onClick = {
                        showBackgroundRationale = false
                        backgroundLocationPermissionState.launchPermissionRequest()
                    }) {
                        Text(strings.grantPermission)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBackgroundRationale = false }) {
                        Text(strings.cancel)
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }
        
        NotificationBanner()
    }
}

@Composable
fun ClientScreenContent(
    trackingId: String,
    connectionStatus: ConnectionStatus,
    connectionError: String?,
    subscribersCount: Int = 0,
    restProgress: Float = 0f,
    hasBackgroundPermission: Boolean = true,
    isBatteryOptimized: Boolean = false,
    onToggleTracking: () -> Unit,
    onRefreshId: () -> Unit = {},
    onUpdateCustomId: (String) -> Unit = {},
    onRequestBackgroundPermission: () -> Unit = {},
    onFixBatteryOptimization: () -> Unit = {},
    onShowDeviceDetails: () -> Unit = {},
    onShowConnectionType: () -> Unit = {},
    onManualUpdate: () -> Unit = {},
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val clipboardManager = LocalClipboardManager.current
    val strings = LocalStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(paddingValues)
            .padding(top = 0.dp, bottom = 16.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Descriptive Header
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.clientPortal,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                LanguageSelector()

                var showOptionsMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showOptionsMenu = true }) {
                        Icon(Icons.Default.MoreVert, "More Options", tint = MaterialTheme.colorScheme.primary)
                    }
                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.connectionType) },
                            leadingIcon = { Icon(Icons.Default.SettingsEthernet, null) },
                            onClick = {
                                showOptionsMenu = false
                                onShowConnectionType()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.deviceInfo) },
                            leadingIcon = { Icon(Icons.Default.Info, null) },
                            onClick = {
                                showOptionsMenu = false
                                onShowDeviceDetails()
                            }
                        )
                    }
                }
            }

            Text(
                text = strings.locationSynchronization,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Warnings
            if (!hasBackgroundPermission || isBatteryOptimized) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!hasBackgroundPermission) {
                        WarningItem(Icons.Default.Warning, strings.backgroundLocationWarning, onRequestBackgroundPermission)
                    }
                    if (isBatteryOptimized) {
                        WarningItem(Icons.Default.BatteryAlert, strings.batteryOptimizationWarning, onFixBatteryOptimization)
                    }
                }
            }
        }

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = connectionStatus,
                label = "TrackingState",
                transitionSpec = {
                    if (targetState == ConnectionStatus.CONNECTED || (initialState == ConnectionStatus.IDLE && (targetState == ConnectionStatus.CONNECTING || targetState == ConnectionStatus.RECONNECTING))) {
                        (slideInVertically { height -> height / 4 } + fadeIn(animationSpec = tween(400)))
                            .togetherWith(slideOutVertically { height -> -height / 4 } + fadeOut(animationSpec = tween(400)))
                    } else {
                        (slideInVertically { height -> -height / 4 } + fadeIn(animationSpec = tween(400)))
                            .togetherWith(slideOutVertically { height -> height / 4 } + fadeOut(animationSpec = tween(400)))
                    }.using(SizeTransform(clip = false))
                }
            ) { status ->
                when (status) {
                    ConnectionStatus.IDLE -> {
                        IdleView(
                            trackingId = trackingId,
                            onStart = onToggleTracking,
                            onRefreshId = onRefreshId,
                            onUpdateCustomId = onUpdateCustomId,
                            isLoading = false
                        )
                    }
                    ConnectionStatus.CONNECTING, ConnectionStatus.RECONNECTING, ConnectionStatus.FAILED -> {
                        ConnectingView(
                            onCancel = onToggleTracking,
                            isFailed = status == ConnectionStatus.FAILED || status == ConnectionStatus.RECONNECTING,
                            errorMessage = connectionError
                        )
                    }
                    ConnectionStatus.CONNECTED -> {
                        TrackingView(
                            trackingId = trackingId,
                            connectionStatus = connectionStatus,
                            subscribersCount = subscribersCount,
                            restProgress = restProgress,
                            onManualUpdate = onManualUpdate,
                            onStop = onToggleTracking,
                            onCopy = { clipboardManager.setText(AnnotatedString(trackingId)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleView(
    trackingId: String,
    onStart: () -> Unit,
    onRefreshId: () -> Unit,
    onUpdateCustomId: (String) -> Unit,
    isLoading: Boolean
) {
    val strings = LocalStrings.current
    val clipboardManager = LocalClipboardManager.current
    var isEditing by remember { mutableStateOf(false) }
    var editValue by remember(trackingId) { 
        mutableStateOf(if (trackingId.startsWith("@")) trackingId.removePrefix("@") else "") 
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth().shadow(2.dp, RoundedCornerShape(20.dp)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            AnimatedContent(targetState = isEditing, label = "IdEdit") { editing ->
                if (editing) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = editValue,
                            onValueChange = { if (it.length <= 20) editValue = it.filter { c -> c.isLetterOrDigit() || c == '_' } },
                            modifier = Modifier.weight(1f),
                            prefix = { Text("@", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                            placeholder = { Text(strings.usernamePlaceholder) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        IconButton(onClick = { if (editValue.length >= 2) { onUpdateCustomId("@$editValue"); isEditing = false } }, enabled = editValue.length >= 2) {
                            Icon(Icons.Default.Done, strings.save, tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { isEditing = false }) {
                            Icon(Icons.Default.Close, strings.cancel, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                                .clickable { clipboardManager.setText(AnnotatedString(trackingId)) }
                                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(if (trackingId.startsWith("@")) Icons.Default.Person else Icons.Default.Fingerprint, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(strings.sessionUuid, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text(if (trackingId.startsWith("@")) trackingId else if (trackingId.length > 13) "${trackingId.take(13)}..." else trackingId,
                                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                        Row(modifier = Modifier.padding(end = 16.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onRefreshId, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Refresh, strings.refresh, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = { isEditing = true }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, strings.edit, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(strings.readyToSync, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(strings.startBroadcastingDesc, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text(strings.startTracking, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ConnectingView(
    onCancel: () -> Unit,
    isFailed: Boolean = false,
    errorMessage: String? = null
) {
    val strings = LocalStrings.current
    val infiniteTransition = rememberInfiniteTransition(label = "ConnectingPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "Scale"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        Box(contentAlignment = Alignment.Center) {
            Surface(modifier = Modifier.size(120.dp).graphicsLayer(scaleX = scale, scaleY = scale), shape = CircleShape, color = (if (isFailed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer).copy(alpha = 0.3f)) {}
            CircularProgressIndicator(modifier = Modifier.size(80.dp), color = if (isFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, strokeWidth = 4.dp)
            Icon(if (isFailed) Icons.Default.CloudOff else Icons.Default.CloudSync, null, modifier = Modifier.size(40.dp), tint = if (isFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(if (isFailed) strings.connecting else strings.connecting, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(errorMessage ?: strings.waitingForConnection, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(48.dp))
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp), shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error), border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)) {
            Text(strings.cancel)
        }
    }
}

@Composable
private fun TrackingView(
    trackingId: String, 
    connectionStatus: ConnectionStatus,
    subscribersCount: Int,
    restProgress: Float = 0f,
    onManualUpdate: () -> Unit,
    onStop: () -> Unit, 
    onCopy: () -> Unit
) {
    val strings = LocalStrings.current
    var lastUpdateMark by remember { mutableStateOf(TimeSource.Monotonic.markNow()) }
    val scale by animateFloatAsState(
        targetValue = if (lastUpdateMark.elapsedNow() < 300.milliseconds) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "IconScale"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale).let { mod ->
                if (SettingsManager.connectionType == SettingsManager.ConnectionType.REST) {
                    mod.clickable(indication = ripple(bounded = false, radius = 60.dp), interactionSource = remember { MutableInteractionSource() }) {
                        lastUpdateMark = TimeSource.Monotonic.markNow()
                        onManualUpdate()
                    }
                } else mod
            }
        ) {
            val statusColor = Color(0xFF2E7D32)
            Surface(modifier = Modifier.size(100.dp), shape = CircleShape, color = statusColor.copy(alpha = 0.1f)) {}
            if (SettingsManager.connectionType == SettingsManager.ConnectionType.REST) {
                CircularProgressIndicator(progress = { restProgress }, modifier = Modifier.size(100.dp), color = statusColor, strokeWidth = 4.dp, trackColor = Color.Transparent)
            }
            Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(48.dp), tint = statusColor)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(strings.nowTracking, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
        
        if (subscribersCount > 0) {
            Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Visibility, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.adminSubscribed(subscribersCount), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)).clickable { onCopy() }.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(strings.sessionUuid, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(trackingId, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(strings.tapToCopyId, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp), shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error), border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)) {
            Text(strings.stopTracking)
        }
    }
}

@Composable
private fun WarningItem(icon: ImageVector, message: String, onFix: () -> Unit) {
    val strings = LocalStrings.current
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.1f))) {
        Row(modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(12.dp))
            Text(message, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onFix, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), modifier = Modifier.height(32.dp), colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text(strings.fix, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PermissionDialog(isPermanentlyDenied: Boolean, onDismiss: () -> Unit, onGrant: () -> Unit, onOpenSettings: () -> Unit) {
    val strings = LocalStrings.current
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (isPermanentlyDenied) strings.permissionBlocked else strings.locationAccessRequired, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }, text = { Text(if (isPermanentlyDenied) strings.locationPermissionDeniedPermanently else strings.locationPermissionRationale, style = MaterialTheme.typography.bodyMedium) }, confirmButton = { Button(onClick = if (isPermanentlyDenied) onOpenSettings else onGrant, shape = RoundedCornerShape(12.dp)) { Text(if (isPermanentlyDenied) strings.openSettings else strings.grantPermission) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }, shape = RoundedCornerShape(24.dp), containerColor = MaterialTheme.colorScheme.surface, icon = { Icon(if (isPermanentlyDenied) Icons.Default.Settings else Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary) })
}

@Composable
fun ConnectionTypeDialog(onDismiss: () -> Unit, onSet: (SettingsManager.ConnectionType) -> Unit) {
    val strings = LocalStrings.current
    var selectedType by remember { mutableStateOf(SettingsManager.connectionType) }
    AlertDialog(onDismissRequest = onDismiss, title = { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.SettingsEthernet, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)); Spacer(modifier = Modifier.width(12.dp)); Text(strings.connectionType, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) } }, text = { Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { SettingsManager.ConnectionType.entries.forEach { type -> val isSelected = type == selectedType; val backgroundColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface); val borderColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)); Surface(onClick = { selectedType = type }, shape = RoundedCornerShape(16.dp), color = backgroundColor, border = BorderStroke(1.dp, borderColor), modifier = Modifier.fillMaxWidth()) { Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(40.dp).background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) { Icon(if (type == SettingsManager.ConnectionType.WEBSOCKET) Icons.Default.SyncAlt else Icons.Default.Http, null, modifier = Modifier.size(20.dp), tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant) }; Spacer(modifier = Modifier.width(16.dp)); Column(modifier = Modifier.weight(1f)) { Text(when (type) { SettingsManager.ConnectionType.WEBSOCKET -> strings.websocket; SettingsManager.ConnectionType.REST -> strings.rest }, style = MaterialTheme.typography.titleMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) }; if (isSelected) { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) } } } } } }, confirmButton = { Button(onClick = { onSet(selectedType) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.height(48.dp).fillMaxWidth(0.45f)) { Text(strings.set, fontWeight = FontWeight.Bold) } }, dismissButton = { OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp), modifier = Modifier.height(48.dp).fillMaxWidth(0.45f)) { Text(strings.close) } }, shape = RoundedCornerShape(28.dp), containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp)
}

@Composable
private fun DeviceInfoItem(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 1
            )
        }
    }
}

@Composable
fun DeviceDetailsDialog(onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    val clipboardManager = LocalClipboardManager.current
    val deviceUuid = SettingsManager.deviceUuid
    val platform = getPlatform()
    AlertDialog(onDismissRequest = onDismiss, title = { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)); Spacer(modifier = Modifier.width(12.dp)); Text(strings.deviceInfo, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) } }, text = { Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { if (platform.deviceName.isNotBlank()) { DeviceInfoItem(strings.deviceName, platform.deviceName, Icons.Default.Person) }; Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { DeviceInfoItem(strings.manufacturer, platform.manufacturer, Icons.Default.Business, Modifier.weight(1f)); DeviceInfoItem(strings.model, platform.model, Icons.Default.PhoneAndroid, Modifier.weight(1f)) }; Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { DeviceInfoItem(strings.platform, platform.name, Icons.Default.Settings, Modifier.weight(1f)); DeviceInfoItem(strings.appVersion, "1.0.0", Icons.Default.Build, Modifier.weight(1f)) }; if (platform.systemId.isNotBlank()) { DeviceInfoItem(strings.systemId, platform.systemId, Icons.Default.Dns) }; Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)).border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(20.dp)).clickable { clipboardManager.setText(AnnotatedString(deviceUuid)) }.padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Fingerprint, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(strings.deviceUuid, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Icon(Icons.Default.ContentCopy, strings.copy, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) }; Text(deviceUuid, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) } } }, confirmButton = { Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) { Text(strings.dismiss) } }, shape = RoundedCornerShape(28.dp), containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp)
}

@Composable
fun StopConfirmationDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val strings = LocalStrings.current
    AlertDialog(onDismissRequest = onDismiss, title = { Text(strings.confirmStopTracking, fontWeight = FontWeight.Bold) }, text = { Text(strings.stopTrackingConfirmationMessage) }, confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), shape = RoundedCornerShape(12.dp)) { Text(strings.stopTracking, fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }, shape = RoundedCornerShape(24.dp), containerColor = MaterialTheme.colorScheme.surface)
}

@Preview
@Composable
fun ClientScreenPreview() {
    MaterialTheme { Surface(color = MaterialTheme.colorScheme.background) { ClientScreenContent(trackingId = "123e4567-e89b-12d3-a456-426614174000", connectionStatus = ConnectionStatus.IDLE, connectionError = null, onToggleTracking = {}) } }
}

@Preview
@Composable
fun ClientScreenTrackingPreview() {
    MaterialTheme { Surface(color = MaterialTheme.colorScheme.background) { ClientScreenContent(trackingId = "123e4567-e89b-12d3-a456-426614174000", connectionStatus = ConnectionStatus.CONNECTED, connectionError = null, subscribersCount = 2, onToggleTracking = {}) } }
}
