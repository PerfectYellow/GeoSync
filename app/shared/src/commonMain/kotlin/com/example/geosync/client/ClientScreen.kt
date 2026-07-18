package com.example.geosync.client

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.example.geosync.localization.LocalStrings
import com.example.geosync.permissions.PermissionNames
import com.example.geosync.permissions.rememberPermissionState
import com.example.geosync.network.ConnectionStatus
import com.example.geosync.network.ConnectivityStatus
import com.example.geosync.network.rememberConnectivityObserver
import com.example.geosync.LanguageSelector
import com.example.geosync.NotificationBanner
import com.example.geosync.NotificationManager

@Composable
fun ClientScreen(
    viewModel: ClientViewModel = viewModel { ClientViewModel() },
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val trackingId by viewModel.trackingId.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val connectionError by viewModel.connectionError.collectAsState()
    val subscribersCount by viewModel.subscribersCount.collectAsState()

    val connectivityObserver = rememberConnectivityObserver()
    val networkStatus by connectivityObserver.observe().collectAsState(ConnectivityStatus.Online)

    var showPermissionRationale by remember { mutableStateOf(false) }

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
            onToggleTracking = {
                if (networkStatus == ConnectivityStatus.Offline) {
                    NotificationManager.showOffline()
                } else if (locationPermissionState.hasPermission) {
                    viewModel.toggleTracking()
                } else {
                    showPermissionRationale = true
                }
            },
            paddingValues = paddingValues
        )

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
        
        // Floating notification at the top
        NotificationBanner()
    }
}

@Composable
fun PermissionDialog(
    isPermanentlyDenied: Boolean,
    onDismiss: () -> Unit,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isPermanentlyDenied) strings.permissionBlocked else strings.locationAccessRequired,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = if (isPermanentlyDenied) {
                    strings.locationPermissionDeniedPermanently
                } else {
                    strings.locationPermissionRationale
                },
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = if (isPermanentlyDenied) onOpenSettings else onGrant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isPermanentlyDenied) strings.openSettings else strings.grantPermission)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                imageVector = if (isPermanentlyDenied) Icons.Default.Settings else Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    )
}

@Composable
fun ClientScreenContent(
    trackingId: String,
    connectionStatus: ConnectionStatus,
    connectionError: String?,
    subscribersCount: Int = 0,
    onToggleTracking: () -> Unit,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val clipboardManager = LocalClipboardManager.current
    val isConnected = connectionStatus == ConnectionStatus.CONNECTED
    val strings = LocalStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(paddingValues)
            .padding(horizontal = 24.dp, vertical = 16.dp),
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
            }

            Text(
                text = strings.locationSynchronization,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isConnected,
                label = "TrackingState",
                transitionSpec = {
                    if (targetState) {
                        (slideInVertically { height -> height / 4 } + fadeIn(animationSpec = tween(400)))
                            .togetherWith(slideOutVertically { height -> -height / 4 } + fadeOut(animationSpec = tween(400)))
                    } else {
                        (slideInVertically { height -> -height / 4 } + fadeIn(animationSpec = tween(400)))
                            .togetherWith(slideOutVertically { height -> height / 4 } + fadeOut(animationSpec = tween(400)))
                    }.using(
                        SizeTransform(clip = false)
                    )
                }
            ) { connected ->
                if (!connected) {
                    IdleView(
                        onStart = onToggleTracking,
                        isLoading = connectionStatus == ConnectionStatus.CONNECTING
                    )
                } else {
                    TrackingView(
                        trackingId = trackingId,
                        connectionStatus = connectionStatus,
                        subscribersCount = subscribersCount,
                        onStop = onToggleTracking,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(trackingId))
                        }
                    )
                }
            }
        }

        // Info card at the bottom
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = strings.dataVisibilityInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun IdleView(onStart: () -> Unit, isLoading: Boolean) {
    val strings = LocalStrings.current
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
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = strings.readyToSync,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = strings.startBroadcastingDesc,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            enabled = !isLoading,
            shape = RoundedCornerShape(20.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(strings.startTracking, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun TrackingView(
    trackingId: String, 
    connectionStatus: ConnectionStatus,
    subscribersCount: Int,
    onStop: () -> Unit, 
    onCopy: () -> Unit
) {
    val strings = LocalStrings.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            val statusColor = Color(0xFF2E7D32)
            
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = statusColor.copy(alpha = 0.1f)
            ) {}
            
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = statusColor
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = strings.nowTracking,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E7D32)
        )
        
        if (subscribersCount > 0) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = strings.adminSubscribed(subscribersCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        AnimatedVisibility(
            visible = connectionStatus == ConnectionStatus.CONNECTED,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .clickable { onCopy() }
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = strings.sessionUuid,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = trackingId,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = strings.tapToCopyId,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        OutlinedButton(
            onClick = onStop,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        ) {
            Text(strings.stopTracking)
        }
    }
}

@Preview
@Composable
fun ClientScreenPreview() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ClientScreenContent(
                trackingId = "123e4567-e89b-12d3-a456-426614174000",
                connectionStatus = ConnectionStatus.IDLE,
                connectionError = null,
                subscribersCount = 0,
                onToggleTracking = {}
            )
        }
    }
}

@Preview
@Composable
fun ClientScreenTrackingPreview() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ClientScreenContent(
                trackingId = "123e4567-e89b-12d3-a456-426614174000",
                connectionStatus = ConnectionStatus.CONNECTED,
                connectionError = null,
                subscribersCount = 1,
                onToggleTracking = {}
            )
        }
    }
}

@Preview
@Composable
fun ClientScreenErrorPreview() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ClientScreenContent(
                trackingId = "",
                connectionStatus = ConnectionStatus.FAILED,
                connectionError = "Connection refused: Server is unreachable",
                subscribersCount = 0,
                onToggleTracking = {}
            )
        }
    }
}
